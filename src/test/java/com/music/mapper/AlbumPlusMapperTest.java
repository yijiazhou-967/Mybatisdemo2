package com.music.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.music.entity.Album;
import com.music.query.AlbumQueries;
import com.music.util.MyBatisUtil;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** 任务四真实 MySQL 验收：每个用例独立插入数据，最终回滚。 */
@Tag("database")
class AlbumPlusMapperTest {
    private SqlSession session;
    private AlbumPlusMapper mapper;
    private String marker;

    @BeforeAll
    static void requireTransactionalTable() throws java.sql.SQLException {
        try (SqlSession session = MyBatisUtil.openSession();
             java.sql.Statement statement = session.getConnection().createStatement();
             java.sql.ResultSet result = statement.executeQuery(
                     "SELECT ENGINE FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'album'")) {
            assertTrue(result.next(), "请先创建 album 表");
            assertEquals("InnoDB", result.getString(1), "测试需要事务型表");
        }
    }

    @BeforeEach
    void openSession() {
        session = MyBatisUtil.openSession();
        mapper = session.getMapper(AlbumPlusMapper.class);
        marker = "mp" + UUID.randomUUID().toString().replace("-", "");
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

    private Album insert(String suffix, String price, String genre, int status) {
        Album album = new Album(marker + suffix, marker, genre, new BigDecimal(price),
                5, LocalDate.of(2024, 1, 1), status);
        assertEquals(1, mapper.insert(album));
        assertNotNull(album.getAlbumId());
        return album;
    }

    private List<Integer> ids(List<Album> albums) {
        return albums.stream().map(Album::getAlbumId).collect(Collectors.toList());
    }

    private LambdaQueryWrapper<Album> ownRecords() {
        return AlbumQueries.lambdaQueryWrapper(null, null, null).eq(Album::getArtist, marker);
    }

    @Test
    void baseMapper_无XML完成CRUD与字段映射() {
        Album original = insert("通用CRUD", "19.90", "流行", 1);
        Album loaded = mapper.selectById(original.getAlbumId());
        assertEquals(original.getTitle(), loaded.getTitle());
        assertEquals(original.getArtist(), loaded.getArtist());
        assertEquals(original.getGenre(), loaded.getGenre());
        assertEquals(original.getPrice(), loaded.getPrice());
        assertEquals(original.getStock(), loaded.getStock());
        assertEquals(original.getReleaseDate(), loaded.getReleaseDate());
        assertEquals(original.getStatus(), loaded.getStatus());
        Album patch = new Album();
        patch.setAlbumId(original.getAlbumId());
        patch.setPrice(new BigDecimal("29.90"));
        patch.setStock(0);
        patch.setStatus(0);
        assertEquals(1, mapper.updateById(patch));
        loaded = mapper.selectById(original.getAlbumId());
        assertEquals(new BigDecimal("29.90"), loaded.getPrice());
        assertEquals(0, loaded.getStock());
        assertEquals(0, loaded.getStatus());
        assertEquals(original.getTitle(), loaded.getTitle());
        assertEquals(1, mapper.deleteById(original.getAlbumId()));
        assertNull(mapper.selectById(original.getAlbumId()));
    }

    @Test
    void wrappers_多条件与范围查询两种写法等效且排序稳定() {
        Album lower = insert("匹配A", "30.00", "流行", 1);
        Album higher = insert("匹配B", "40.00", "流行", 1);
        Album tied = insert("匹配C", "40.00", "流行", 1);
        insert("匹配低价", "29.99", "流行", 1);
        insert("匹配其他风格", "99.00", "摇滚", 1);
        insert("匹配下架", "99.00", "流行", 0);
        insert("其他名称", "99.00", "流行", 1);
        BigDecimal minimum = new BigDecimal("30.00");
        List<Integer> stringResult = ids(mapper.selectList(AlbumQueries.queryWrapper(marker + "匹配", "流行", minimum)));
        List<Integer> lambdaResult = ids(mapper.selectList(AlbumQueries.lambdaQueryWrapper(marker + "匹配", "流行", minimum)));
        assertEquals(Arrays.asList(higher.getAlbumId(), tied.getAlbumId(), lower.getAlbumId()), stringResult);
        assertEquals(stringResult, lambdaResult);
    }

    @Test
    void wrappers_空白条件不参与筛选但仍排除下架记录() {
        Album active = insert("空白条件", "0.00", "实验风格", 1);
        Album inactive = insert("下架", "99.00", "实验风格", 0);
        List<Album> found = mapper.selectList(AlbumQueries.lambdaQueryWrapper(" ", "\t", null)
                .eq(Album::getArtist, marker));
        assertEquals(Arrays.asList(active.getAlbumId()), ids(found));
        assertNotNull(mapper.selectById(inactive.getAlbumId()), "下架状态不是逻辑删除");
    }

    @Test
    void pagination_首页中间页末页条数总数页数和偏移正确() {
        for (int index = 0; index < 7; index++) {
            insert("分页" + index, "10.00", "实验风格", 1);
        }
        insert("分页下架", "10.00", "实验风格", 0);
        List<Integer> all = ids(mapper.selectList(ownRecords()));
        for (int current = 1; current <= 3; current++) {
            Page<Album> page = mapper.selectPage(new Page<>(current, 3), ownRecords());
            assertEquals(7, page.getTotal());
            assertEquals(3, page.getPages());
            assertEquals(current, page.getCurrent());
            assertEquals(3, page.getSize());
            int start = (current - 1) * 3;
            assertEquals(all.subList(start, Math.min(start + 3, all.size())), ids(page.getRecords()));
        }
    }

    @Test
    void pagination_超过总页数返回空记录而不重置页码() {
        insert("越界页", "10.00", "实验风格", 1);
        Page<Album> page = mapper.selectPage(new Page<>(99, 3), ownRecords());
        assertEquals(1, page.getTotal());
        assertEquals(1, page.getPages());
        assertEquals(99, page.getCurrent());
        assertTrue(page.getRecords().isEmpty());
    }

    @Test
    void pagination_无匹配数据返回零记录零页数() {
        Page<Album> page = mapper.selectPage(new Page<>(1, 3), ownRecords());
        assertEquals(0, page.getTotal());
        assertEquals(0, page.getPages());
        assertTrue(page.getRecords().isEmpty());
    }

    @Test
    void coexist_原XML和通用Mapper在同一事务读取一致且可回滚() {
        Album original = insert("共存", "19.90", "流行", 1);
        AlbumMapper legacy = session.getMapper(AlbumMapper.class);
        assertEquals(original.getTitle(), legacy.selectById(original.getAlbumId()).getTitle());
        original.setPrice(new BigDecimal("39.90"));
        assertEquals(1, legacy.update(original));
        assertEquals(new BigDecimal("39.90"), mapper.selectById(original.getAlbumId()).getPrice());
        session.rollback();
        try (SqlSession verification = MyBatisUtil.openSession()) {
            assertNull(verification.getMapper(AlbumPlusMapper.class).selectById(original.getAlbumId()));
        }
    }
}
