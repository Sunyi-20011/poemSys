package com.poemSys.user.service.forum;

import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.poemSys.common.bean.Result;
import com.poemSys.common.entity.basic.SysPost;
import com.poemSys.common.entity.connection.ConUserPost;
import com.poemSys.common.service.ConUserPostService;
import com.poemSys.common.service.SysPostService;
import com.poemSys.user.service.general.GetLoginSysUserService;
import com.poemSys.user.bean.Form.AddPostForm;
import com.poemSys.user.service.general.ImageUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
public class AddPostService
{
    @Autowired
    GetLoginSysUserService getLoginSysUserService;

    @Autowired
    SysPostService sysPostService;

    @Autowired
    ConUserPostService conUserPostService;

    @Autowired
    ImageUploadService imageUploadService;

    @Autowired
    ContentCheckService contentCheckService;

    public Result add(AddPostForm addPostForm)
    {
        Long userId = getLoginSysUserService.getSysUser().getId();

        String title = addPostForm.getTitle();
        String content = addPostForm.getContent();
        MultipartFile coverImage = addPostForm.getCoverImage();
        String uuid = UUID.randomUUID().toString();
        String imagePath = null;

        String titleCheckRes = contentCheckService.KMPCheckout(title);
        String contentCheckRes = contentCheckService.KMPCheckout(content);
        if(!titleCheckRes.equals("pass")||!contentCheckRes.equals("pass"))
        {
            if(titleCheckRes.equals("pass"))
                titleCheckRes = "";
            if(contentCheckRes.equals("pass"))
                contentCheckRes = "";
            return new Result(1, "帖子提交失败,您的提交内容含有敏感词:" + titleCheckRes+" "+contentCheckRes, null);
        }
        if(coverImage!=null)
        {
            Result uploadRes = imageUploadService.upload(coverImage, "/images/forum/");
            if (uploadRes.getCode() != 0)
                return uploadRes;
            imagePath = uploadRes.getData().toString();
        }
        sysPostService.save(new SysPost(title, content, LocalDateTime.now(), 0,
                0, imagePath, uuid));
        SysPost sysPost = sysPostService.getOne(new QueryWrapper<SysPost>()
                .eq("uuid", uuid));
        conUserPostService.save(new ConUserPost(userId, sysPost.getId()));
        return new Result(0, "帖子发布成功", null);
    }
}
