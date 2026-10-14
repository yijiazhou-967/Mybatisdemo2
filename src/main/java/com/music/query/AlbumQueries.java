package com.music.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.music.entity.Album;

import java.math.BigDecimal;

/** 任务四步骤 4.4：字符串字段与 Lambda 两种等效条件构造器写法。 */
public final class AlbumQueries {
    private AlbumQueries() {
    }

    /** 标题模糊、风格精确、最低价格可选；只查在售，价格降序、主键升序。 */
    public static QueryWrapper<Album> queryWrapper(String title, String genre, BigDecimal minPrice) {
        return new QueryWrapper<Album>()
                .like(StringUtils.isNotBlank(title), "title", title)
                .eq(StringUtils.isNotBlank(genre), "genre", genre)
                .eq("status", 1)
                .ge(minPrice != null, "price", minPrice)
                .orderByDesc("price")
                .orderByAsc("album_id");
    }

    /** Lambda 引用实体属性，避免手写字段名；每次调用创建独立构造器。 */
    public static LambdaQueryWrapper<Album> lambdaQueryWrapper(String title, String genre, BigDecimal minPrice) {
        return new LambdaQueryWrapper<Album>()
                .like(StringUtils.isNotBlank(title), Album::getTitle, title)
                .eq(StringUtils.isNotBlank(genre), Album::getGenre, genre)
                .eq(Album::getStatus, 1)
                .ge(minPrice != null, Album::getPrice, minPrice)
                .orderByDesc(Album::getPrice)
                .orderByAsc(Album::getAlbumId);
    }
}
