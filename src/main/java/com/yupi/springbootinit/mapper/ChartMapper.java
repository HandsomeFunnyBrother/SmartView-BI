package com.yupi.springbootinit.mapper;

import com.yupi.springbootinit.model.entity.Chart;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
* @author zxk
* @description 针对表【chart(图表信息表)】的数据库操作Mapper
* @createDate 2024-12-19 20:12:12
* @Entity com.yupi.springbootinit.model.entity.Chart
*/
public interface ChartMapper extends BaseMapper<Chart> {


    //todo List<map<String,object>> queryChartData(String querySQL);
    //todo create 分库分表优化项目数据库
}




