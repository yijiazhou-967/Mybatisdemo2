package com.music;

import com.music.entity.Album;
import com.music.mapper.AlbumMapper;
import com.music.util.MyBatisUtil;
import org.apache.ibatis.session.SqlSession;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 第一个 MyBatis 程序（任务二 步骤 2.4）。
 * 演示：读取配置 -> 构建 SqlSessionFactory（工具类封装）-> 开启会话 -> 获取 Mapper 代理 -> 执行查询。
 */
public class MusicStoreDemo {

    public static void main(String[] args) {
        String mode = args.length == 0 ? "list" : args[0];
        if (!Arrays.asList("list", "crud", "dynamic").contains(mode)) {
            throw new IllegalArgumentException("参数应为 list、crud 或 dynamic");
        }
        // try-with-resources：会话用完自动关闭
        try (SqlSession session = MyBatisUtil.openSession()) {
            AlbumMapper mapper = session.getMapper(AlbumMapper.class);

            try {
                if ("crud".equals(mode)) {
                    demonstrateCrud(mapper);
                } else if ("dynamic".equals(mode)) {
                    demonstrateDynamic(mapper);
                } else {
                    List<Album> albums = mapper.selectAll();
                    System.out.println("专辑总数：" + albums.size());
                    albums.forEach(System.out::println);
                }
            } finally {
                // 练习程序不持久保存临时记录；正式业务增删改须显式 session.commit()。
                session.rollback();
            }
        }
    }

    private static Album example(String title, String artist) {
        return new Album(title, artist, "实验风格", new BigDecimal("19.90"),
                10, LocalDate.of(2024, 1, 1), 1);
    }

    private static void demonstrateCrud(AlbumMapper mapper) {
        Album album = example("CRUD演示", "demo" + UUID.randomUUID().toString().replace("-", ""));
        System.out.println("插入行数：" + mapper.insert(album) + "，回填主键：" + album.getAlbumId());
        System.out.println("按主键查询：" + mapper.selectById(album.getAlbumId()));
        album.setPrice(new BigDecimal("29.90"));
        System.out.println("整体更新行数：" + mapper.update(album));
        System.out.println("更新后查询：" + mapper.selectById(album.getAlbumId()));
        System.out.println("删除行数：" + mapper.deleteById(album.getAlbumId()));
        System.out.println("删除后查询：" + mapper.selectById(album.getAlbumId()));
    }

    private static void demonstrateDynamic(AlbumMapper mapper) {
        String artist = "demo" + UUID.randomUUID().toString().replace("-", "");
        List<Album> batch = Arrays.asList(example("动态SQL演示A", artist), example("动态SQL演示B", artist));
        System.out.println("批量插入行数：" + mapper.insertBatch(batch));
        Album condition = new Album();
        condition.setArtist(artist);
        condition.setGenre("实验风格");
        condition.setStatus(1);
        List<Album> found = mapper.selectByCondition(condition);
        System.out.println("where 多条件查询：" + found);
        System.out.println("trim 等效查询：" + mapper.selectByConditionWithTrim(condition));
        Album patch = new Album();
        patch.setAlbumId(found.get(0).getAlbumId());
        patch.setStock(0);
        patch.setStatus(0);
        System.out.println("动态更新行数：" + mapper.updateSelective(patch));
        Album branch = new Album();
        branch.setArtist(artist);
        System.out.println("choose 歌手分支：" + mapper.selectSmart(branch));
        System.out.println("批量删除行数：" + mapper.deleteBatch(found.stream()
                .map(Album::getAlbumId).collect(Collectors.toList())));
    }
}
