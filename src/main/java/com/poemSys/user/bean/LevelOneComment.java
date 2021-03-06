package com.poemSys.user.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LevelOneComment implements Serializable
{
    private static final long serialVersionUID = 1L;

    private long id;
    private UserInfo ownerUserInfo;
    private String content;
    private LocalDateTime createdTime;
    private boolean isOwner;
    private List<SonComment> sonComments;
}
