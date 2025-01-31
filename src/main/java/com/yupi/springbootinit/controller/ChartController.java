package com.yupi.springbootinit.controller;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.springbootinit.annotation.AuthCheck;
import com.yupi.springbootinit.bizmq.BiMessageProducer;
import com.yupi.springbootinit.common.BaseResponse;
import com.yupi.springbootinit.common.DeleteRequest;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.common.ResultUtils;
import com.yupi.springbootinit.constant.CommonConstant;
import com.yupi.springbootinit.constant.FileConstant;
import com.yupi.springbootinit.constant.UserConstant;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.exception.ThrowUtils;
import com.yupi.springbootinit.manager.AIManager;
import com.yupi.springbootinit.manager.DouBaoManager;
import com.yupi.springbootinit.manager.RedisLimiterManager;
import com.yupi.springbootinit.model.dto.chart.*;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.model.entity.User;
import com.yupi.springbootinit.model.vo.BiResponse;
import com.yupi.springbootinit.service.ChartService;
import com.yupi.springbootinit.service.UserService;
import com.yupi.springbootinit.utils.ExcelUtils;
import com.yupi.springbootinit.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

import static com.yupi.springbootinit.constant.ChartStatusConstant.*;

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
    private AIManager aiManager;
    @Resource
    private RedisLimiterManager redisLimiterManager;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;
    @Resource
    private BiMessageProducer biMessageProducer;
    //新增豆包
    @Resource
    private DouBaoManager douBaoManager;

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
    public BaseResponse<Chart> getChartVOById(long id, HttpServletRequest request) {
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
     * 分页获取列表（仅管理员）
     *
     * @param chartQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<Chart>> listChartVOByPage(@RequestBody ChartQueryRequest chartQueryRequest,
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
     * 分页获取当前用户创建的图表信息
     * zhegeclfldf
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<Chart>> listMyChartVOByPage(@RequestBody ChartQueryRequest chartQueryRequest,
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
     * 智能分析
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen")
// 把返回值改成BiResponse
    public BaseResponse<BiResponse> genChartByAi(@RequestPart("file") MultipartFile multipartFile,
                                                 GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();

        // 校验
        // 如果分析目标为空，就抛出请求参数错误异常，并给出提示
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        // 如果名称不为空，并且名称长度大于100，就抛出异常，并给出提示
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");

        /**
         * 校验文件
         * 拿到用户请求的文件
         * 取到原始文件大小
         */
        long size = multipartFile.getSize();
        //取到原始文件名字
        String originalFilename = multipartFile.getOriginalFilename();

        /**
         *  校验文件大小
         *  定义常量表示1MB
         */
        final long ONE_MB = 1024 * 1024;
        // 如果文件大小大于1MB，就抛出异常，并给出提示
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件大小超过1MB");

        /**
         * 校验文件后缀
         * 利用FileUtil工具类中的getSuffix获取文件后缀
         */
        String suffix = FileUtil.getSuffix(originalFilename);
        //定义合法后缀列表
        final List<String> validFinalSuffixList = Arrays.asList("xls", "xlsx", "csv");
        // 如果文件后缀不在合法后缀列表中，就抛出异常，并给出提示
        ThrowUtils.throwIf(!validFinalSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件格式错误");

        // 通过response对象拿到用户id(必须登录才能使用)
        User loginUser = userService.getLoginUser(request);

        //限流判断，每一个用户一个限流器
        redisLimiterManager.doRateLimit("genChartByAi:" + loginUser.getId());

        // 构造用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");

        // 拼接分析目标
        String userGoal = goal;
        // 如果图表类型不为空
        if (StringUtils.isNotBlank(chartType)) {
            // 就将分析目标拼接上“请使用”+图表类型
            userGoal += "，请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据：").append("\n");
        // 压缩后的数据（把multipartFile传进来）
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append(csvData).append("\n");

        // 拿到返回结果
        String result = douBaoManager.performStandardChatCompletion(userInput.toString());
        //String result = aiManager.sendMsgToXingHuo(true, userInput.toString());

        // 对返回结果做拆分,按照5个中括号进行拆分
        String[] splits = result.split("【【【【【");
        // 拆分之后还要进行校验
        if (splits.length < 3) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成错误");
        }

        // 从返回的结果中提取生成的图表，去除前后的空格
        String genChart = splits[1].trim();
        // 从返回的结果中提取生成的结果，去除前后的空格
        String genResult = splits[2].trim();

        // 插入到数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setUserId(loginUser.getId());
        chart.setStatus(SUCCEED);
        boolean saveResult = chartService.save(chart);
        // 如果图表保存失败，抛出系统错误
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");
        //VO返回
        BiResponse biResponse = new BiResponse();
        biResponse.setGenChart(genChart);
        biResponse.setGenResult(genResult);
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);
    }

    /**
     * 智能分析(异步）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/async")
// 把返回值改成BiResponse
    public BaseResponse<BiResponse> genChartByAiAsync(@RequestPart("file") MultipartFile multipartFile,
                                                 GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();

        // 校验
        // 如果分析目标为空，就抛出请求参数错误异常，并给出提示
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        // 如果名称不为空，并且名称长度大于100，就抛出异常，并给出提示
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");

        /**
         * 校验文件
         * 拿到用户请求的文件
         * 取到原始文件大小
         */
        long size = multipartFile.getSize();
        //取到原始文件名字
        String originalFilename = multipartFile.getOriginalFilename();

        /**
         *  校验文件大小
         *  定义常量表示1MB
         */
        final long ONE_MB = 1024 * 1024;
        // 如果文件大小大于1MB，就抛出异常，并给出提示
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件大小超过1MB");

        /*
          校验文件后缀
          利用FileUtil工具类中的getSuffix获取文件后缀
         */
        String suffix = FileUtil.getSuffix(originalFilename);
        //定义合法后缀列表
        final List<String> validFinalSuffixList = Arrays.asList("xls", "xlsx", "csv");
        // 如果文件后缀不在合法后缀列表中，就抛出异常，并给出提示
        ThrowUtils.throwIf(!validFinalSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件格式错误");

        // 通过response对象拿到用户id(必须登录才能使用)
        User loginUser = userService.getLoginUser(request);

        //限流判断，每一个用户一个限流器
        redisLimiterManager.doRateLimit("genChartByAi:" + loginUser.getId());

        // 构造用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");

        // 拼接分析目标
        String userGoal = goal;
        // 如果图表类型不为空
        if (StringUtils.isNotBlank(chartType)) {
            // 就将分析目标拼接上“请使用”+图表类型
            userGoal += "，请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据：").append("\n");
        // 压缩后的数据（把multipartFile传进来）
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append(csvData).append("\n");

        //先保存到数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
//        chart.setGenChart(genChart);
//        chart.setGenResult(genResult);
        //设置状态为排队中
        chart.setStatus(WAIT);
        chart.setUserId(loginUser.getId());
        boolean saveResult = chartService.save(chart);
        // 如果图表保存失败，抛出系统错误
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        //最终的返回结果前提交下一个任务
        CompletableFuture.runAsync(()->{
            //状态改为执行中
            Chart updateChart = new Chart();
            updateChart.setId(chart.getId());
            updateChart.setStatus(RUNNING);
            boolean b = chartService.updateById(updateChart);
            if(!b){
                handleChartUpdateError(chart.getId(),"更新图表执行状态失败");
                return;
            }
            // 调用 AI
            String result = douBaoManager.performStandardChatCompletion(userInput.toString());
            //String result = aiManager.sendMsgToXingHuo(true, userInput.toString());
            // 对返回结果做拆分,按照5个中括号进行拆分
            String[] splits = result.split("【【【【【");
            // 拆分之后还要进行校验
            if (splits.length < 3) {
                handleChartUpdateError(chart.getId(),"AI 生成错误");
                return;
            }
            // 从返回的结果中提取生成的图表，去除前后的空格
            String genChart = splits[1].trim();
            // 从返回的结果中提取生成的结果，去除前后的空格
            String genResult = splits[2].trim();
            //得到结果后，再次更新
            Chart updateChartResult = new Chart();
            updateChartResult.setId(chart.getId());
            updateChartResult.setGenChart(genChart);
            updateChartResult.setGenResult(genResult);
            updateChartResult.setStatus(SUCCEED);
            boolean updateResult = chartService.updateById(updateChartResult);
            if (!updateResult) {
                handleChartUpdateError(chart.getId(),"更新图表成功状态失败");
            }
        },threadPoolExecutor);

        //VO返回
        BiResponse biResponse = new BiResponse();
//        biResponse.setGenChart(genChart);
//        biResponse.setGenResult(genResult);
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);
    }

    /**
     * 智能分析(异步消息队列）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/async/mq")
// 把返回值改成BiResponse
    public BaseResponse<BiResponse> genChartByAiAsyncMq(@RequestPart("file") MultipartFile multipartFile,
                                                      GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();

        // 校验
        // 如果分析目标为空，就抛出请求参数错误异常，并给出提示
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        // 如果名称不为空，并且名称长度大于100，就抛出异常，并给出提示
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");

        /**
         * 校验文件
         * 拿到用户请求的文件
         * 取到原始文件大小
         */
        long size = multipartFile.getSize();
        //取到原始文件名字
        String originalFilename = multipartFile.getOriginalFilename();

        /**
         *  校验文件大小
         *  定义常量表示1MB
         */
        final long ONE_MB = 1024 * 1024;
        // 如果文件大小大于1MB，就抛出异常，并给出提示
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件大小超过1MB");

        /*
          校验文件后缀
          利用FileUtil工具类中的getSuffix获取文件后缀
         */
        String suffix = FileUtil.getSuffix(originalFilename);
        //定义合法后缀列表
        final List<String> validFinalSuffixList = Arrays.asList("xls", "xlsx", "csv");
        // 如果文件后缀不在合法后缀列表中，就抛出异常，并给出提示
        ThrowUtils.throwIf(!validFinalSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件格式错误");

        // 通过response对象拿到用户id(必须登录才能使用)
        User loginUser = userService.getLoginUser(request);

        //限流判断，每一个用户一个限流器
        redisLimiterManager.doRateLimit("genChartByAi:" + loginUser.getId());

        // 构造用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");

        // 拼接分析目标
        String userGoal = goal;
        // 如果图表类型不为空
        if (StringUtils.isNotBlank(chartType)) {
            // 就将分析目标拼接上“请使用”+图表类型
            userGoal += "，请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据：").append("\n");
        // 压缩后的数据（把multipartFile传进来）
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append(csvData).append("\n");

        //先保存到数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
//        chart.setGenChart(genChart);
//        chart.setGenResult(genResult);
        //设置状态为排队中
        chart.setStatus(WAIT);
        chart.setUserId(loginUser.getId());
        boolean saveResult = chartService.save(chart);
        // 如果图表保存失败，抛出系统错误
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        //生产者发送图表id给消费者
        Long newChartId = chart.getId();
        biMessageProducer.sendMessage(String.valueOf(newChartId));

        //VO返回
        BiResponse biResponse = new BiResponse();
//        biResponse.setGenChart(genChart);
//        biResponse.setGenResult(genResult);
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);
    }


    //更新图表状态异常错误，工具类
    private  void handleChartUpdateError(long chartId,String execMessage){
        Chart updateChart = new Chart();
        updateChart.setId(chartId);
        updateChart.setStatus(FAILED);
        updateChart.setExecMessage(execMessage);
        boolean updateResult = chartService.updateById(updateChart);
        if (!updateResult) {
            log.error("更新图表失败，chartId：{}，execMessage：{}", chartId, execMessage);
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
        String genResult = chartQueryRequest.getGenResult();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();

        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.eq(StringUtils.isNotBlank(genResult), "genResult", genResult);
        queryWrapper.eq(StringUtils.isNotBlank(chartType), "chartType", chartType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

}
