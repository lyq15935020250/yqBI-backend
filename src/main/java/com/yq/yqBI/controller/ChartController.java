package com.yq.yqBI.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.yq.yqBI.annotation.AuthCheck;
import com.yq.yqBI.common.BaseResponse;
import com.yq.yqBI.common.DeleteRequest;
import com.yq.yqBI.common.ErrorCode;
import com.yq.yqBI.common.ResultUtils;
import com.yq.yqBI.constant.CommonConstant;
import com.yq.yqBI.constant.UserConstant;
import com.yq.yqBI.exception.BusinessException;
import com.yq.yqBI.exception.ThrowUtils;
import com.yq.yqBI.manager.AiManager;
import com.yq.yqBI.manager.RedisLimiterManager;
import com.yq.yqBI.model.dto.chart.*;
import com.yq.yqBI.model.entity.Chart;
import com.yq.yqBI.model.entity.User;
import com.yq.yqBI.model.enums.ChartStatusEnum;
import com.yq.yqBI.model.vo.BiResponse;
import com.yq.yqBI.service.ChartService;
import com.yq.yqBI.service.UserService;
import com.yq.yqBI.utils.ExcelUtils;
import com.yq.yqBI.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 帖子接口
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private AiManager aiManager;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private RedisLimiterManager redisLimiterManager;

    private final static Gson GSON = new Gson();

    private final long BiModelId = 1765029821865742338L;

    // region 增删改查

    /**
     * 创建
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);
        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
            HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
            HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    // endregion

    /**
     * 编辑（用户）
     *
     * @param chartEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);
        User loginUser = userService.getLoginUser(request);
        long id = chartEditRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }



    /**
     * 智能分析（同步）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen")
    public BaseResponse<BiResponse> genChartByAi(@RequestPart("file") MultipartFile multipartFile,
                                             GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {

        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        // 校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");

        // 校验文件大小和后缀
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();

        final long maxSize = 1024 * 1024L;
        ThrowUtils.throwIf(size > maxSize, ErrorCode.PARAMS_ERROR, "文件大小超过1M");

        // 利用 hutool 工具类得到文件的后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> suffixList = Arrays.asList("csv", "xlsx", "xls");
        ThrowUtils.throwIf(!suffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件格式不正确");

        User loginUser = userService.getLoginUser(request);

        // 限流
        redisLimiterManager.doRateLimit("genChartByAI_" + loginUser.getId());

        // AI 预设
//        final String prompt = "你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容:\n" +
//                "分析需求:\n" +
//                "{数据分析的需求或者目标}\n" +
//                "原始数据:\n" +
//                "{csv格式的原始数据，用,作为分隔符}\n" +
//                "请根据这两部分内容，按照以下指定格式生成内容（此外不要输出任何多余的开头、结尾、注释)\n" +
//                "【【【【【\n" +
//                "{前端Echarts V5的 option配置对象js代码，合理地将数据进行可视化，不要生成任何多余的内容，比如注释}\n" +
//                "【【【【【\n" +
//                "{明确的数据分析结论、越详细越好，不要生成多余的注释}";

//        分析需求:
//        分析网站用户的增长情况
//        原始数据:
//        日期,用户数
//        1号,10
//        2号,20
//        3号,30

        // 用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");

        String userGoal = goal;
        if (StrUtil.isNotBlank(chartType)){
            userGoal += ",请使用" + chartType;
        }

        userInput.append(userGoal).append("\n");
        userInput.append("原始数据:").append("\n");
        // 压缩后的数据
        String csvDate = ExcelUtils.excelToCsv(multipartFile);
        userInput.append(csvDate).append("\n");

        // 获取到 AI 响应结果
        String result = aiManager.doChart(BiModelId, userInput.toString());

        String[] split = result.split("【【【【【");
        if (split.length < 3){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"AI 生成错误");
        }

        String genChart = split[1].trim();
        String genResult = split[2].trim();

        // 插入数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvDate);
        chart.setChartType(chartType);
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setUserId(loginUser.getId());

        boolean save = chartService.save(chart);
        ThrowUtils.throwIf(!save,ErrorCode.SYSTEM_ERROR,"图表保存失败");

        BiResponse biResponse = new BiResponse();
        biResponse.setGenChart(genChart);
        biResponse.setGenResult(genResult);
        biResponse.setChartId(chart.getId());

        return ResultUtils.success(biResponse);

    }

    /**
     * 智能分析（异步）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/async")
    public BaseResponse<BiResponse> genChartByAiAsync(@RequestPart("file") MultipartFile multipartFile,
                                                 GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {

        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        // 校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");

        // 校验文件大小和后缀
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();

        final long maxSize = 1024 * 1024L;
        ThrowUtils.throwIf(size > maxSize, ErrorCode.PARAMS_ERROR, "文件大小超过1M");

        // 利用 hutool 工具类得到文件的后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> suffixList = Arrays.asList("csv", "xlsx", "xls");
        ThrowUtils.throwIf(!suffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件格式不正确");

        User loginUser = userService.getLoginUser(request);

        // 限流
        redisLimiterManager.doRateLimit("genChartByAI_" + loginUser.getId());

        // AI 预设
//        final String prompt = "你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容:\n" +
//                "分析需求:\n" +
//                "{数据分析的需求或者目标}\n" +
//                "原始数据:\n" +
//                "{csv格式的原始数据，用,作为分隔符}\n" +
//                "请根据这两部分内容，按照以下指定格式生成内容（此外不要输出任何多余的开头、结尾、注释)\n" +
//                "【【【【【\n" +
//                "{前端Echarts V5的 option配置对象js代码，合理地将数据进行可视化，不要生成任何多余的内容，比如注释}\n" +
//                "【【【【【\n" +
//                "{明确的数据分析结论、越详细越好，不要生成多余的注释}";

//        分析需求:
//        分析网站用户的增长情况
//        原始数据:
//        日期,用户数
//        1号,10
//        2号,20
//        3号,30

        // 用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");

        String userGoal = goal;
        if (StrUtil.isNotBlank(chartType)){
            userGoal += ",请使用" + chartType;
        }

        userInput.append(userGoal).append("\n");
        userInput.append("原始数据:").append("\n");
        // 压缩后的数据
        String csvDate = ExcelUtils.excelToCsv(multipartFile);
        userInput.append(csvDate).append("\n");


        // 插入数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvDate);
        chart.setChartType(chartType);
        // 设置任务状态码为等待中
        chart.setStatus(ChartStatusEnum.WAIT.getValue());
        chart.setUserId(loginUser.getId());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult,ErrorCode.SYSTEM_ERROR,"图表保存失败");

        try {
            CompletableFuture.runAsync(() -> {
                // 第二次跟新数据库，设置任务状态为执行中
                Chart runChart = new Chart();
                runChart.setId(chart.getId());
                runChart.setStatus(ChartStatusEnum.RUNNING.getValue());
                boolean b = chartService.updateById(runChart);
                // 一般不会出错，除非数据库出问题
                if (!b){
                    handlerChartUpdateError(chart.getId(), "更新图表任务为执行中更新失败");
                    return;
                }

                // 获取到 AI 响应结果
                String result = aiManager.doChart(BiModelId, userInput.toString());

                String[] split = result.split("【【【【【");
                if (split.length < 3){
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR,"AI 生成错误");
                }

                String genChart = split[1].trim();
                String genResult = split[2].trim();

                // 第三次更新数据库，得到调用 AI 的结果后，更新图表详情、分析结论、图表执行状态为 success
                Chart chartResult = new Chart();
                chartResult.setId(chart.getId());
                chartResult.setGenChart(genChart);
                chartResult.setGenResult(genResult);
                chartResult.setStatus(ChartStatusEnum.SUCCEED.getValue());
                boolean updateResult = chartService.updateById(chartResult);
                if (!updateResult){
                    handlerChartUpdateError(chart.getId(), "更新图表状态为成功更新失败");
                }

            }, threadPoolExecutor);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"图表生成失败，" + e.getMessage());
        }




        BiResponse biResponse = new BiResponse();
//        biResponse.setGenChart(genChart);
//        biResponse.setGenResult(genResult);
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);

    }

    private void handlerChartUpdateError(Long chartId,String execMessage){
        Chart chart = new Chart();
        chart.setId(chartId);
        chart.setStatus(ChartStatusEnum.FAILED.getValue());
        chart.setGenChart(execMessage);
        boolean b = chartService.updateById(chart);
        if (!b){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"修改失败状态失败");
        }
    }


    /**
     * 获取查询包装类
     *
     * @param chartQueryRequest
     * @return
     */
    private QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chartQueryRequest.getId();
        String name = chartQueryRequest.getName();
        String goal = chartQueryRequest.getGoal();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();

        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.eq(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.eq(StringUtils.isNotBlank(chartType), "chartType", chartType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }


}
