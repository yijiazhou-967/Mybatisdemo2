package com.music.mapper;

import com.music.entity.Album;
import com.music.util.MyBatisUtil;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/** 不连接数据库：验证实际 MyBatis 动态 SQL、绑定参数及 Mapper 参数保护。 */
class AlbumMapperSqlTest {
    private final Configuration configuration = MyBatisUtil.getSqlSessionFactory().getConfiguration();

    private BoundSql bound(String method, Object parameter) {
        return configuration.getMappedStatement("com.music.mapper.AlbumMapper." + method).getBoundSql(parameter);
    }

    private String sql(String method, Object parameter) {
        return bound(method, parameter).getSql().replaceAll("\\s+", " ").trim();
    }

    @Test
    void configuration_每个抽象方法都有映射且结果字段齐全() {
        Arrays.stream(AlbumMapper.class.getMethods()).filter(method -> !method.isDefault())
                .forEach(method -> assertTrue(configuration.hasStatement("com.music.mapper.AlbumMapper." + method.getName())));
        assertEquals(8, configuration.getResultMap("com.music.mapper.AlbumMapper.albumMap").getResultMappings().size());
        assertTrue(configuration.isMapUnderscoreToCamelCase());
    }

    @Test
    void where_trim等效并正确绑定四个条件() {
        Album condition = new Album();
        condition.setTitle("' OR 1=1 --");
        condition.setArtist("歌手");
        condition.setGenre("流行");
        condition.setStatus(0);
        String where = sql("selectByCondition", condition);
        assertEquals(where, sql("selectByConditionWithTrim", condition));
        assertTrue(where.contains("WHERE title LIKE CONCAT('%', ?, '%') AND artist LIKE CONCAT('%', ?, '%') AND genre = ? AND status = ?"));
        assertFalse(where.contains(condition.getTitle()));
        assertEquals(4, bound("selectByCondition", condition).getParameterMappings().size());
    }

    @Test
    void where_空条件不生成多余WHERE且零状态不被忽略() {
        Album condition = new Album();
        condition.setTitle("");
        assertFalse(sql("selectByCondition", condition).contains("WHERE"));
        assertEquals(sql("selectByCondition", null), sql("selectByConditionWithTrim", null));
        condition.setStatus(0);
        assertTrue(sql("selectByCondition", condition).contains("WHERE status = ?"));
    }

    @Test
    void choose_标题优先否则歌手无条件查全部() {
        Album condition = new Album();
        condition.setTitle("标题");
        condition.setArtist("歌手");
        assertTrue(sql("selectSmart", condition).contains("WHERE title = ?"));
        assertFalse(sql("selectSmart", condition).contains("artist = ?"));
        condition.setTitle("");
        assertTrue(sql("selectSmart", condition).contains("WHERE artist = ?"));
        assertTrue(sql("selectSmart", null).contains("WHERE 1 = 1"));
        assertFalse(sql("selectSmart", new Album()).contains("status ="));
    }

    @Test
    void set_仅生成有值字段且无末尾逗号() {
        Album patch = new Album();
        patch.setAlbumId(1);
        patch.setStock(0);
        patch.setStatus(0);
        assertEquals("UPDATE album SET stock = ?, status = ? WHERE album_id = ?", sql("updateSelectiveRows", patch));
        assertEquals(3, bound("updateSelectiveRows", patch).getParameterMappings().size());
    }

    @Test
    void foreach_生成正确占位符与绑定数量() {
        assertEquals("DELETE FROM album WHERE album_id IN ( ? , ? )",
                sql("deleteBatchRows", Collections.singletonMap("ids", Arrays.asList(1, 2))));
        BoundSql batch = bound("insertBatchRows", Collections.singletonMap("albums", Arrays.asList(new Album(), new Album())));
        assertEquals(14, batch.getParameterMappings().size());
        assertEquals(2, batch.getParameterMappings().stream().filter(p -> p.getProperty().endsWith(".releaseDate")).count());
    }

    @Test
    void emptyBatch_返回零且无需建立数据库连接() {
        try (SqlSession session = MyBatisUtil.openSession()) {
            AlbumMapper mapper = session.getMapper(AlbumMapper.class);
            assertEquals(0, mapper.insertBatch(Collections.emptyList()));
            assertEquals(0, mapper.deleteBatch(Collections.emptyList()));
        }
    }

    @Test
    void invalidBatch_在执行SQL之前拒绝无效参数() {
        try (SqlSession session = MyBatisUtil.openSession()) {
            AlbumMapper mapper = session.getMapper(AlbumMapper.class);
            assertThrows(IllegalArgumentException.class, () -> mapper.insertBatch(null));
            assertThrows(IllegalArgumentException.class, () -> mapper.insertBatch(Arrays.asList(new Album(), null)));
            assertThrows(IllegalArgumentException.class, () -> mapper.deleteBatch(null));
            assertThrows(IllegalArgumentException.class, () -> mapper.deleteBatch(Arrays.asList(1, null)));
            assertThrows(IllegalArgumentException.class, () -> mapper.deleteBatch(Arrays.asList(0)));
        }
    }

    @Test
    void invalidPatch_拒绝缺少主键或更新字段() {
        try (SqlSession session = MyBatisUtil.openSession()) {
            AlbumMapper mapper = session.getMapper(AlbumMapper.class);
            assertThrows(IllegalArgumentException.class, () -> mapper.updateSelective(null));
            Album patch = new Album();
            patch.setStock(0);
            assertThrows(IllegalArgumentException.class, () -> mapper.updateSelective(patch));
            patch.setAlbumId(0);
            assertThrows(IllegalArgumentException.class, () -> mapper.updateSelective(patch));
            Album empty = new Album();
            empty.setAlbumId(1);
            assertThrows(IllegalArgumentException.class, () -> mapper.updateSelective(empty));
        }
    }
}
