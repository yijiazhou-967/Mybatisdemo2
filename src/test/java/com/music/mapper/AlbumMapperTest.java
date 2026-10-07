package com.music.mapper;

import com.music.entity.Album;
import com.music.util.MyBatisUtil;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AlbumMapper 单元测试（JUnit 5）。
 * 覆盖任务二基础 CRUD 与任务三动态 SQL；每个用例自建数据、自清理，可反复执行。
 */
class AlbumMapperTest {

    /** 构造一条测试专辑，artist 用唯一标记以便定向清理 */
    private Album newAlbum(String title, String artist) {
        return new Album(title, artist, "测试风格",
                new BigDecimal("9.90"), 1, LocalDate.of(2024, 1, 1), 1);
    }

    /** 清理指定歌手的全部测试记录，返回删除行数 */
    private int cleanupByArtist(SqlSession session, String artist) {
        AlbumMapper mapper = session.getMapper(AlbumMapper.class);
        Album cond = new Album();
        cond.setArtist(artist);
        List<Integer> ids = mapper.selectByCondition(cond).stream()
                .map(Album::getAlbumId)
                .collect(Collectors.toList());
        return ids.isEmpty() ? 0 : mapper.deleteBatch(ids);
    }

    // ---------- 任务二：基础 CRUD ----------

    @Test
    void insert_回填主键并可查询() {
        try (SqlSession session = MyBatisUtil.openSession()) {
            AlbumMapper mapper = session.getMapper(AlbumMapper.class);

            Album album = newAlbum("JUnit插入专辑", "单测歌手");
            assertEquals(1, mapper.insert(album));
            session.commit();

            assertNotNull(album.getAlbumId(), "自增主键应回填");
            Album loaded = mapper.selectById(album.getAlbumId());
            assertNotNull(loaded);
            assertEquals("JUnit插入专辑", loaded.getTitle());

            // 清理
            mapper.deleteById(album.getAlbumId());
            session.commit();
        }
    }

    @Test
    void selectAll_返回全部专辑() {
        try (SqlSession session = MyBatisUtil.openSession()) {
            List<Album> list = session.getMapper(AlbumMapper.class).selectAll();
            assertNotNull(list);
            assertTrue(list.size() >= 5, "初始化数据至少 5 条");
        }
    }

    @Test
    void update_整体更新() {
        try (SqlSession session = MyBatisUtil.openSession()) {
            AlbumMapper mapper = session.getMapper(AlbumMapper.class);

            Album album = newAlbum("JUnit整体更新", "单测歌手");
            mapper.insert(album);
            session.commit();
            Integer id = album.getAlbumId();

            album.setPrice(new BigDecimal("88.80"));
            album.setStock(88);
            assertEquals(1, mapper.update(album));
            session.commit();

            Album loaded = mapper.selectById(id);
            assertEquals(new BigDecimal("88.80"), loaded.getPrice());
            assertEquals(88, loaded.getStock());

            mapper.deleteById(id);
            session.commit();
        }
    }

    // ---------- 任务三：动态 SQL ----------

    @Test
    void selectByCondition_多条件模糊查询() {
        try (SqlSession session = MyBatisUtil.openSession()) {
            AlbumMapper mapper = session.getMapper(AlbumMapper.class);

            // 风格精确 + 状态精确
            Album cond = new Album();
            cond.setGenre("流行");
            cond.setStatus(1);
            List<Album> list = mapper.selectByCondition(cond);
            assertFalse(list.isEmpty());
            assertTrue(list.stream().allMatch(a -> "流行".equals(a.getGenre()) && a.getStatus() == 1));

            // 歌手模糊
            Album cond2 = new Album();
            cond2.setArtist("周杰伦");
            List<Album> list2 = mapper.selectByCondition(cond2);
            assertFalse(list2.isEmpty());
            assertTrue(list2.stream().allMatch(a -> a.getArtist().contains("周杰伦")));
        }
    }

    @Test
    void updateSelective_动态更新部分字段() {
        try (SqlSession session = MyBatisUtil.openSession()) {
            AlbumMapper mapper = session.getMapper(AlbumMapper.class);

            Album album = newAlbum("JUnit动态更新", "单测歌手");
            mapper.insert(album);
            session.commit();
            Integer id = album.getAlbumId();

            // 只改价格与库存，其余字段保持原值
            Album patch = new Album();
            patch.setAlbumId(id);
            patch.setPrice(new BigDecimal("66.60"));
            patch.setStock(66);
            assertEquals(1, mapper.updateSelective(patch));
            session.commit();

            Album loaded = mapper.selectById(id);
            assertEquals(new BigDecimal("66.60"), loaded.getPrice());
            assertEquals(66, loaded.getStock());
            assertEquals("单测歌手", loaded.getArtist(), "未更新字段应保持原值");

            mapper.deleteById(id);
            session.commit();
        }
    }

    @Test
    void insertBatch_批量插入与批量删除() {
        try (SqlSession session = MyBatisUtil.openSession()) {
            AlbumMapper mapper = session.getMapper(AlbumMapper.class);

            List<Album> batch = Arrays.asList(
                    newAlbum("批量插入A", "批量歌手"),
                    newAlbum("批量插入B", "批量歌手"),
                    newAlbum("批量插入C", "批量歌手")
            );
            assertEquals(3, mapper.insertBatch(batch));
            session.commit();

            // 按标记查出刚插入的 id，再批量删除
            int deleted = cleanupByArtist(session, "批量歌手");
            assertEquals(3, deleted);
            session.commit();
        }
    }

    @Test
    void selectSmart_分支查询() {
        try (SqlSession session = MyBatisUtil.openSession()) {
            AlbumMapper mapper = session.getMapper(AlbumMapper.class);

            // 有标题 -> 按标题精确
            Album c1 = new Album();
            c1.setTitle("晴天");
            List<Album> r1 = mapper.selectSmart(c1);
            assertEquals(1, r1.size());
            assertEquals("晴天", r1.get(0).getTitle());

            // 无标题有歌手 -> 按歌手精确
            Album c2 = new Album();
            c2.setArtist("Queen");
            List<Album> r2 = mapper.selectSmart(c2);
            assertEquals(1, r2.size());
            assertEquals("Queen", r2.get(0).getArtist());

            // 都没有 -> 只查在售
            List<Album> r3 = mapper.selectSmart(new Album());
            assertFalse(r3.isEmpty());
            assertTrue(r3.stream().allMatch(a -> a.getStatus() == 1));
        }
    }
}
