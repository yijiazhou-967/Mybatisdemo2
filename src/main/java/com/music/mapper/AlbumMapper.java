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
    Album selectById(@Param("albumId") Integer albumId);

    /** 新增专辑，自增主键回填到 albumId */
    int insert(Album album);

    /** 按主键整体更新专辑 */
    int update(Album album);

    /** 按主键删除专辑 */
    int deleteById(@Param("albumId") Integer albumId);

    // ============ 任务三：动态 SQL ============

    /** 多条件模糊查询：标题/歌手模糊、风格精确、状态精确，条件可为空 */
    List<Album> selectByCondition(Album condition);

    /** 与 where 写法等效的 trim 演示，便于核对结果 */
    List<Album> selectByConditionWithTrim(Album condition);

    /** 动态更新：仅更新非空字段（set 标签） */
    default int updateSelective(Album album) {
        if (album == null || album.getAlbumId() == null || album.getAlbumId() <= 0) {
            throw new IllegalArgumentException("动态更新必须提供有效的专辑主键");
        }
        if (album.getTitle() == null && album.getArtist() == null && album.getGenre() == null
                && album.getPrice() == null && album.getStock() == null
                && album.getReleaseDate() == null && album.getStatus() == null) {
            throw new IllegalArgumentException("动态更新必须提供至少一个待更新字段");
        }
        return updateSelectiveRows(album);
    }

    /** XML 执行入口；业务调用使用带校验的 updateSelective */
    int updateSelectiveRows(Album album);

    /** 批量删除（foreach 标签） */
    default int deleteBatch(List<Integer> ids) {
        if (ids == null) {
            throw new IllegalArgumentException("删除主键列表不能为 null");
        }
        if (ids.isEmpty()) {
            return 0;
        }
        if (ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("删除列表必须全部为有效主键");
        }
        return deleteBatchRows(ids);
    }

    /** XML 执行入口；业务调用使用带校验的 deleteBatch */
    int deleteBatchRows(@Param("ids") List<Integer> ids);

    /** 批量插入（foreach 标签） */
    default int insertBatch(List<Album> albums) {
        if (albums == null) {
            throw new IllegalArgumentException("插入专辑列表不能为 null");
        }
        if (albums.isEmpty()) {
            return 0;
        }
        if (albums.stream().anyMatch(album -> album == null)) {
            throw new IllegalArgumentException("插入列表不能包含 null 专辑");
        }
        return insertBatchRows(albums);
    }

    /** XML 执行入口；业务调用使用带校验的 insertBatch */
    int insertBatchRows(@Param("albums") List<Album> albums);

    /** 分支查询：优先按标题精确，其次按歌手精确，否则查询全部 */
    List<Album> selectSmart(Album condition);
}
