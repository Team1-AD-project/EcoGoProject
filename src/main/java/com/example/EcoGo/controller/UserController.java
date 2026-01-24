package com.example.EcoGo.controller;
import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.dto.UserResponseDto;
import com.example.EcoGo.interfacemethods.UserInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口控制器
 * 路径规范：/api/v1/user/xxx（v1表示版本，便于后续迭代）
 */
@RestController
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    // 注入Service接口（而非实现类，符合面向接口编程）
    @Autowired
    private UserInterface userService;

    /**
     * 根据用户名查询用户信息（标准REST API风格）
     * 请求示例：http://localhost:8080/api/v1/user/test     //根据Controller的前缀来
     */
    @GetMapping("/api/v1/user/{username}") // 路径参数替代URL参数
    public ResponseMessage<UserResponseDto> getUserByUsername(
            // 必传参数，为空时返回参数错误
            @PathVariable String username) {

        logger.info("开始查询用户信息，用户名：{}", username);
        UserResponseDto userDTO = userService.getUserByUsername(username);
        logger.debug("查询用户信息成功，用户：{}", userDTO);

        // 返回统一响应格式
        return ResponseMessage.success(userDTO);
    }
}