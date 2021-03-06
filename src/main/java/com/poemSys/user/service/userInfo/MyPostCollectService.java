package com.poemSys.user.service.userInfo;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.poemSys.admin.bean.PageListRes;
import com.poemSys.common.bean.Result;
import com.poemSys.common.entity.basic.SysPost;
import com.poemSys.common.entity.connection.ConUserPostCollect;
import com.poemSys.common.service.ConUserPostCollectService;
import com.poemSys.common.service.SysPostService;
import com.poemSys.user.service.general.GetLoginSysUserService;
import com.poemSys.user.bean.PostPageAns;
import com.poemSys.user.service.general.PostPageAnsProService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MyPostCollectService
{
    @Autowired
    GetLoginSysUserService getLoginSysUserService;

    @Autowired
    ConUserPostCollectService conUserPostCollectService;

    @Autowired
    SysPostService sysPostService;

    @Autowired
    PostPageAnsProService postPageAnsProService;

    public Result getPageList(Integer page, Integer size)
    {
        Long id = getLoginSysUserService.getSysUser().getId();
        Page<SysPost> postPage = new Page<>(page, size);

        List<ConUserPostCollect> con = conUserPostCollectService.list(new QueryWrapper<ConUserPostCollect>()
                .eq("user_id", id));
        List<Long> postIds = new ArrayList<>();
        con.forEach(l-> postIds.add(l.getPostId()));
        if(postIds.isEmpty())
            return new Result(0, "分页获取用户收藏帖子列表成功,共0条", new PageListRes(
                    0L, size.longValue(), page.longValue(), 0L, null
            ));
        Page<SysPost> pageAns = sysPostService.page(postPage, new QueryWrapper<SysPost>()
                .in("id", postIds).orderByDesc("created_time"));
        PostPageAns finalAns = postPageAnsProService.pro(pageAns);
        return new Result(0, "分页获取用户收藏帖子列表成功,共"+finalAns.getTotal()+"条", finalAns);
    }
}
