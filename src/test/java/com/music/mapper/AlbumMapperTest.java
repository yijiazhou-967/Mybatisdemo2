package com.music.mapper;

import com.music.entity.Album;
import com.music.util.MyBatisUtil;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** 真实 MySQL 测试：独立创建数据，整个用例使用一个事务，结束时回滚。 */
@Tag("database")
class AlbumMapperTest {
    private SqlSession session;
    private AlbumMapper mapper;
    private String marker;

    @BeforeAll
    static void requireTransactionalTable() throws java.sql.SQLException {
        // 回滚测试要求事务型表；先核实引擎，避免在非事务表中留下测试数据。
        try (SqlSession session = MyBatisUtil.openSession();
             java.sql.Statement statement = session.getConnection().createStatement();
             java.sql.ResultSet result = statement.executeQuery(
                     "SELECT ENGINE FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'album'")) {
            assertTrue(result.next(), "请先执行 sql/init.sql 创建 album 表");
            assertEquals("InnoDB", result.getString(1), "数据库测试需要 InnoDB 表以保证事务回滚");
        }
    }

    @BeforeEach
    void openSession() {
        session = MyBatisUtil.openSession();
        mapper = session.getMapper(AlbumMapper.class);
        marker = "test" + UUID.randomUUID().toString().replace("-", "");
    }

    @AfterEach
    void rollbackAndClose() {
        if (session != null) {
            try {
                session.rollback();
            } finally {
                session.close();
            }
        }
    }

    private Album album(String suffix, int status) {
        return new Album(marker + suffix, marker, "测试风格",
                new BigDecimal("9.90"), 1, LocalDate.of(2024, 1, 1), status);
    }

    private Album insert(String suffix, int status) {
        Album album = album(suffix, status);
        assertEquals(1, mapper.insert(album));
        assertNotNull(album.getAlbumId());
        return album;
    }

    private List<Integer> ids(List<Album> albums) {
        return albums.stream().map(Album::getAlbumId).collect(Collectors.toList());
    }

    @Test
    void insert_回填主键并映射全部字段() {
        Album original = insert("插入", 0);
        Album loaded = mapper.selectById(original.getAlbumId());
        assertNotNull(loaded);
        assertEquals(original.getAlbumId(), loaded.getAlbumId());
        assertEquals(original.getTitle(), loaded.getTitle());
        assertEquals(original.getArtist(), loaded.getArtist());
        assertEquals(original.getGenre(), loaded.getGenre());
        assertEquals(original.getPrice(), loaded.getPrice());
        assertEquals(original.getStock(), loaded.getStock());
        assertEquals(original.getReleaseDate(), loaded.getReleaseDate());
        assertEquals(0, loaded.getStatus());
    }

    @Test
    void insert_发行日期允许为空() {
        Album original = album("无日期", 1);
        original.setReleaseDate(null);
        assertEquals(1, mapper.insert(original));
        assertNull(mapper.selectById(original.getAlbumId()).getReleaseDate());
    }

    @Test
    void selectAll_包含自建数据且主键升序() {
        Album a = insert("查询A", 1);
        Album b = insert("查询B", 0);
        List<Integer> result = ids(mapper.selectAll());
        assertTrue(result.containsAll(Arrays.asList(a.getAlbumId(), b.getAlbumId())));
        assertEquals(result.stream().sorted().collect(Collectors.toList()), result);
    }

    @Test
    void update_整体更新并可将发行日期置空() {
        Album original = insert("更新前", 1);
        original.setTitle(marker + "更新后");
        original.setArtist(marker + "新歌手");
        original.setGenre("摇滚");
        original.setPrice(new BigDecimal("88.80"));
        original.setStock(0);
        original.setReleaseDate(null);
        original.setStatus(0);
        assertEquals(1, mapper.update(original));
        Album loaded = mapper.selectById(original.getAlbumId());
        assertEquals(original.getTitle(), loaded.getTitle());
        assertEquals(original.getArtist(), loaded.getArtist());
        assertEquals("摇滚", loaded.getGenre());
        assertEquals(new BigDecimal("88.80"), loaded.getPrice());
        assertEquals(0, loaded.getStock());
        assertEquals(0, loaded.getStatus());
        assertNull(loaded.getReleaseDate());
    }

    @Test
    void deleteById_删除后查询为空再次删除返回零() {
        Album original = insert("删除", 1);
        assertEquals(1, mapper.deleteById(original.getAlbumId()));
        assertNull(mapper.selectById(original.getAlbumId()));
        assertEquals(0, mapper.deleteById(original.getAlbumId()));
    }

    @Test
    void selectByCondition_条件叠加支持零状态与模糊查询() {
        Album match = insert("匹配", 0);
        insert("不匹配状态", 1);
        Album other = album("不匹配风格", 0);
        other.setGenre("其他风格");
        mapper.insert(other);
        Album condition = new Album();
        condition.setTitle(marker);
        condition.setArtist(marker.substring(4));
        condition.setGenre("测试风格");
        condition.setStatus(0);
        assertEquals(Arrays.asList(match.getAlbumId()), ids(mapper.selectByCondition(condition)));
        assertEquals(ids(mapper.selectByCondition(condition)), ids(mapper.selectByConditionWithTrim(condition)));
    }

    @Test
    void selectByCondition_空条件与trim写法均返回全部() {
        insert("空条件", 1);
        List<Integer> all = ids(mapper.selectAll());
        assertEquals(all, ids(mapper.selectByCondition(new Album())));
        assertEquals(all, ids(mapper.selectByCondition(null)));
        assertEquals(all, ids(mapper.selectByConditionWithTrim(new Album())));
    }

    @Test
    void updateSelective_仅更新非空字段并支持零值() {
        Album original = insert("局部更新", 1);
        Album patch = new Album();
        patch.setAlbumId(original.getAlbumId());
        patch.setPrice(new BigDecimal("66.60"));
        patch.setStock(0);
        patch.setStatus(0);
        assertEquals(1, mapper.updateSelective(patch));
        Album loaded = mapper.selectById(original.getAlbumId());
        assertEquals(new BigDecimal("66.60"), loaded.getPrice());
        assertEquals(0, loaded.getStock());
        assertEquals(0, loaded.getStatus());
        assertEquals(original.getTitle(), loaded.getTitle());
        assertEquals(original.getArtist(), loaded.getArtist());
        assertEquals(original.getReleaseDate(), loaded.getReleaseDate());
    }

    @Test
    void insertBatch_批量插入删除只影响本用例记录() {
        Album survivor = insert("保留", 1);
        List<Album> batch = Arrays.asList(album("批量A", 1), album("批量B", 0), album("批量C", 1));
        assertEquals(3, mapper.insertBatch(batch));
        Album condition = new Album();
        condition.setTitle(marker + "批量");
        List<Integer> inserted = ids(mapper.selectByCondition(condition));
        assertEquals(3, inserted.size());
        assertEquals(3, mapper.deleteBatch(inserted));
        for (Integer id : inserted) {
            assertNull(mapper.selectById(id));
        }
        assertNotNull(mapper.selectById(survivor.getAlbumId()));
    }

    @Test
    void selectSmart_标题优先其次歌手无条件查全部() {
        Album first = insert("分支A", 1);
        Album second = insert("分支B", 0);
        Album title = new Album();
        title.setTitle(first.getTitle());
        title.setArtist("不会匹配的歌手");
        assertEquals(Arrays.asList(first.getAlbumId()), ids(mapper.selectSmart(title)));
        Album artist = new Album();
        artist.setArtist(marker);
        assertEquals(Arrays.asList(first.getAlbumId(), second.getAlbumId()), ids(mapper.selectSmart(artist)));
        assertEquals(ids(mapper.selectAll()), ids(mapper.selectSmart(new Album())));
        assertEquals(ids(mapper.selectAll()), ids(mapper.selectSmart(null)));
    }

    @Test
    void rollback_插入记录不会被持久保存() {
        Album original = insert("回滚", 1);
        session.rollback();
        try (SqlSession verification = MyBatisUtil.openSession()) {
            assertNull(verification.getMapper(AlbumMapper.class).selectById(original.getAlbumId()));
        }
    }
}
