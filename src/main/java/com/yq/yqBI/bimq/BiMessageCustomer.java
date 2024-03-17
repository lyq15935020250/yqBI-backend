package com.yq.yqBI.bimq;

import cn.hutool.core.util.StrUtil;
import com.rabbitmq.client.Channel;
import com.yq.yqBI.common.ErrorCode;
import com.yq.yqBI.constant.AiConstant;
import com.yq.yqBI.exception.BusinessException;
import com.yq.yqBI.manager.AiManager;
import com.yq.yqBI.model.entity.Chart;
import com.yq.yqBI.model.enums.ChartStatusEnum;
import com.yq.yqBI.service.ChartService;
import com.yq.yqBI.utils.ExcelUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;

/**
 * @author lyq
 * @description:
 * @date 2024/3/17 17:24
 */
@Component
@Slf4j
public class BiMessageCustomer {

    @Resource
    private ChartService chartService;

    @Resource
    private AiManager aiManager;

    /**
     *
     * @param message 接受到的消息
     * @param channel 消息所在的通道，可以通过通道与 RabbitMQ 进行交互
     * @param deliveryTag 消息投递的标签，消息的唯一标识
     * @Description:
     *              @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag 是一个方法注解，用于获取消息的唯一投递标签，赋值给 long deliveryTag
     *              RabbitMQ 中的每一条消息都会被分配一个唯一的投递标签，用于标识该消息在通道中的投递状态和顺序，
     */
    @SneakyThrows
    @RabbitListener(queues = {BiMqConstant.BI_QUEUE_NAME},ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag){
        log.info("receive message：{}", message);

        if (StrUtil.isBlank(message)){
            // 消息为空，拒绝当前消息，让消息重新进入队列
            channel.basicNack(deliveryTag,false,false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"消费者接受到的消息为空");
        }

        long chartId = Long.parseLong(message);
        Chart chart = chartService.getById(chartId);
        if (chart == null){
            // 图表为空，拒绝当前消息，让消息重新进入队列
            channel.basicNack(deliveryTag,false,false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"图表为空");
        }

        // 第二次跟新数据库，设置任务状态为执行中
        Chart runChart = new Chart();
        runChart.setId(chart.getId());
        runChart.setStatus(ChartStatusEnum.RUNNING.getValue());
        boolean b = chartService.updateById(runChart);
        // 一般不会出错，除非数据库出问题
        if (!b){
            channel.basicNack(deliveryTag,false,false);
            handlerChartUpdateError(chart.getId(), "更新图表任务为执行中更新失败");
            return;
        }

        // 获取到 AI 响应结果
        String result = aiManager.doChart(AiConstant.BiModelId, buildUserInput(chart));

        String[] split = result.split("【【【【【");
        if (split.length < 3){
            channel.basicNack(deliveryTag,false,false);
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
            channel.basicNack(deliveryTag,false,false);
            handlerChartUpdateError(chart.getId(), "更新图表状态为成功更新失败");
        }
        channel.basicAck(deliveryTag, false);
    }

    private String buildUserInput(Chart chart){

        String goal = chart.getGoal();
        String chartType = chart.getChartType();
        String csvDate = chart.getChartData();

        // 用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");

        String userGoal = goal;
        if (StrUtil.isNotBlank(chartType)){
            userGoal += ",请使用" + chartType;
        }

        userInput.append(userGoal).append("\n");
        userInput.append("原始数据:").append("\n");
        userInput.append(csvDate).append("\n");

        return userInput.toString();
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

}
