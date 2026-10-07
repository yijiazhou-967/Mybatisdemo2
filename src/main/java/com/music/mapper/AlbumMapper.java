package com.music.mapper;

import com.music.entity.Album;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 音乐专辑持久层接口，对应映射文件 AlbumMapper.xml。
 * 任务二：基础 CRUD；任务三：动态 SQL、多条件查询与批量操作。
 */
public interface AlbumMapper {

    // ============ 任务二：基础 CRUD ============

    /** 查询全部专辑 */
    List<Album> selectAll();

    /** 按主键查询单条专辑 */
    Album selectById(Integer albumId);

    /** 新增专辑，自增主键回填到 albumId */
    int insert(Album album);

    /** 按主键整体更新专辑 */
    int update(Album album);

    /** 按主键删除专辑 */
    int deleteById(Integer albumId);

    // ============ 任务三：动态 SQL ============

    /** 多条件模糊查询：标题/歌手模糊、风格精确、状态精确，条件可为空 */
    List<Album> selectByCondition(Album condition);

    /** 动态更新：仅更新非空字段（set 标签） */
    int updateSelective(Album album);

    /** 批量删除（foreach 标签） */
    int deleteBatch(@Param("ids") List<Integer> ids);

    /** 批量插入（foreach 标签） */
    int insertBatch(@Param("albums") List<Album> albums);

    /** 分支查询：优先按标题精确，其次按歌手精确，否则查在售（choose/when/otherwise） */
    List<Album> selectSmart(Album condition);
}
