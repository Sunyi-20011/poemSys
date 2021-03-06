package com.poemSys.admin.controller;

import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.poemSys.admin.bean.Form.*;
import com.poemSys.admin.service.userManage.UserListInfoPageAnsProcessService;
import com.poemSys.admin.service.userManage.SearchUserService;
import com.poemSys.admin.service.userManage.UpdateUserInfoService;
import com.poemSys.admin.bean.PageListRes;
import com.poemSys.common.bean.Result;
import com.poemSys.common.entity.basic.SysMessage;
import com.poemSys.common.service.SysMessageService;
import com.poemSys.user.bean.WebsocketMsg;
import com.poemSys.user.service.general.GetLoginSysUserService;
import com.poemSys.common.entity.basic.SysUser;
import com.poemSys.common.service.SysUserService;
import com.poemSys.user.service.general.UpdateRedisSysUserService;
import com.poemSys.user.service.general.WebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 后台用户管理模块
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
public class UserManageController
{
    @Autowired
    SysUserService sysUserService;

    @Autowired
    UpdateUserInfoService updateUserInfoService;

    @Autowired
    GetLoginSysUserService getLoginSysUserService;

    @Autowired
    UserListInfoPageAnsProcessService userListInfoPageAnsProcessService;

    @Autowired
    SearchUserService searchUserService;

    @Autowired
    WebSocketService webSocketService;

    @Autowired
    UpdateRedisSysUserService updateRedisSysUserService;

    @Autowired
    SysMessageService sysMessageService;

    @PreAuthorize("hasRole('admin')")
    @GetMapping("/partUserList/{page}/{size}")
    public Result partUserList(@PathVariable("page") Integer page,
                                  @PathVariable("size") Integer size)
    {
        sysUserService.updateUserState();
        Page<SysUser> userPage = new Page<>(page, size);
        //分页查询结果
        Page<SysUser> pageAns = sysUserService.page(userPage);
        //重新封装查询结果
        PageListRes finalAns = userListInfoPageAnsProcessService.pro(pageAns);
        return new Result(0, "用户部分列表获取成功", finalAns);
    }

    @PreAuthorize("hasRole('admin')")
    @PostMapping("/updateUserInfo")
    public Result updateUserInfo(@RequestBody UpdateUserInfoForm userInfo)
    {
        return updateUserInfoService.update(userInfo);
    }

    @PreAuthorize("hasRole('admin')")
    @PostMapping("/deleteUser")
    public Result deleteUser(@RequestBody IdForm deleteUserForm)
    {
        long id = deleteUserForm.getId();
        if(id==1)
            return new Result(-2, "无法删除系统默认用户", null);
        sysUserService.deleteSysUserById(id);
        return new Result(0, "用户删除成功", null);
    }

    @PreAuthorize("hasRole('admin')")
    @PostMapping("/deleteUsers")
    public Result deleteUsers(@RequestBody IdsForm idsForm)
    {
        List<IdForm> userForms = idsForm.getIds();
        userForms.forEach(userForm ->{
            if(userForm.getId()!=1)
            sysUserService.deleteSysUserById(userForm.getId());
        });

        return new Result(0, "用户批量删除成功", null);
    }

    @PreAuthorize("hasRole('admin')")
    @PostMapping("/lockUser")
    public Result lockUser(@RequestBody LockUserForm lockUserForm)
    {
        try
        {
            Long loginUserId = getLoginSysUserService.getSysUser().getId();
            Long userId = lockUserForm.getId();
            if(userId==1)
                return new Result(-2, "无法锁定系统默认用户", null);
            Integer lockMinutes = lockUserForm.getLockMinutes();
            SysUser sysUser = sysUserService.getSysUserById(userId);
            if (lockMinutes == -1)
                sysUser.setUnlockTime(LocalDateTime.parse("9999-12-31 23:59:59",
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            else
                sysUser.setUnlockTime(LocalDateTime.now().plusMinutes(lockMinutes));
            sysUser.setState(false);
            sysUser.setLockReason(lockUserForm.getLockReason());
            sysUser.setLockAdmin(getLoginSysUserService.getSysUser().getUsername());
            sysUserService.updateById(sysUser);

            sysMessageService.save(new SysMessage(userId, false, 4,
                    0, "对您的账号进行了封禁", loginUserId, true, LocalDateTime.now(),
                    UUID.randomUUID().toString(),0));
            webSocketService.sendMessageOnServer(userId,
                    new WebsocketMsg(4, loginUserId,
                            userId, "管理员"+getLoginSysUserService.getSysUser().getUsername()+"封了你的号"));
            updateRedisSysUserService.updateById(userId);

            return new Result(0, "用户锁定成功", null);
        }catch (NullPointerException e)
        {
            log.error("/admin/lockUser请求参数为空");
            return new Result(-3, "请求参数格式出错", null);
        }
    }

    @PreAuthorize("hasRole('admin')")
    @PostMapping("/unlockUser")
    public Result unlockUser(@RequestBody UnlockUserForm unlockUserForm)
    {
        SysUser sysUser = sysUserService.getSysUserById(unlockUserForm.getId());
        sysUser.setState(true);
        sysUser.setUnlockTime(null);
        sysUser.setLockReason(null);
        sysUser.setLockAdmin(null);
        sysUserService.updateById(sysUser);
        updateRedisSysUserService.updateById(unlockUserForm.getId());

        return new Result(0, "用户解锁成功", null);
    }

    @PreAuthorize("hasRole('admin')")
    @PostMapping("/searchUser")
    public Result searchUser(@RequestBody SearchForm searchUserForm)
    {

        PageListRes searchRes = searchUserService.search(searchUserForm);
        return new Result(0, "用户查询成功，共"+searchRes.getTotal()+"条", searchRes);
    }
}
