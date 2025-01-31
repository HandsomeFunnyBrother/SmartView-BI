package com.yupi.springbootinit.constant;

/**
 * 用户常量
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
public interface UserConstant {

    /**
     * 用户登录态键
     */
    String USER_LOGIN_STATE = "user_login";

    //  region 权限

    /**
     * 默认角色
     */
    String DEFAULT_ROLE = "user";

    /**
     * 管理员角色
     */
    String ADMIN_ROLE = "admin";

    /**
     * 被封号
     */
    String BAN_ROLE = "ban";

    /**
     * 用户昵称
     */
    String USER_NICK_NAME_PREFIX = "小分析员";

    /**
     * 小分析员
     */
    String IMAGE_UPLOAD_DIR = "https://handsomefunnyboy.oss-cn-beijing.aliyuncs.com/common/%E5%B0%8F%E5%88%86%E6%9E%90%E5%B8%88.jpg";
}
