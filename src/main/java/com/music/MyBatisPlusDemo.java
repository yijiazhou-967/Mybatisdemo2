package com.music;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.music.entity.Album;
import com.music.mapper.AlbumPlusMapper;
import com.music.query.AlbumQueries;
import com.music.util.MyBatisUtil;
import org.apache.ibatis.session.SqlSession;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

/** 任务四演示：BaseMapper CRUD、QueryWrapper/Lambda 条件查询与每页 3 条的分页。 */
public final class MyBatisPlusDemo {
    private MyBatisPlusDemo() {
    }

    public static void main(String[] args) {
        String mode = args.length == 0 ? "all" : args[0];
        if (!Arrays.asList("all", "crud", "query", "page").contains(mode)) {
            throw new IllegalArgumentException("参数应为 all、crud、query 或 page");
        }
        try (SqlSession session = MyBatisUtil.openSession()) {
            AlbumPlusMapper mapper = session.getMapper(AlbumPlusMapper.class);
            try {
                if ("all".equals(mode) || "crud".equals(mode)) {
                    demonstrateCrud(mapper);
                }
                if ("all".equals(mode) || "query".equals(mode)) {
                    System.out.println("QueryWrapper（标题含晴、流行、价格至少30元）：");
                    mapper.selectList(AlbumQueries.queryWrapper("晴", "流行", new BigDecimal("30.00")))
                            .forEach(System.out::println);
                    System.out.println("LambdaQueryWrapper（相同条件）：");
                    mapper.selectList(AlbumQueries.lambdaQueryWrapper("晴", "流行", new BigDecimal("30.00")))
                            .forEach(System.out::println);
                }
                if ("all".equals(mode) || "page".equals(mode)) {
                    Page<Album> result = mapper.selectPage(new Page<>(1, 3),
                            AlbumQueries.lambdaQueryWrapper(null, null, null));
                    System.out.println("第 " + result.getCurrent() + " 页，每页 " + result.getSize() + " 条");
                    System.out.println("总记录数 = " + result.getTotal());
                    System.out.println("总页数 = " + result.getPages());
                    result.getRecords().forEach(System.out::println);
                }
            } finally {
                // 实验不保存临时记录，正式业务需要持久化时才调用 commit()。
                session.rollback();
            }
        }
    }

    private static void demonstrateCrud(AlbumPlusMapper mapper) {
        Album album = new Album("MP通用CRUD演示", "mpdemo" + UUID.randomUUID().toString().replace("-", ""),
                "实验风格", new BigDecimal("19.90"), 10, LocalDate.of(2024, 1, 1), 1);
        System.out.println("MP插入行数 = " + mapper.insert(album) + "，回填主键 = " + album.getAlbumId());
        System.out.println("MP按主键查询 = " + mapper.selectById(album.getAlbumId()));
        Album patch = new Album();
        patch.setAlbumId(album.getAlbumId());
        patch.setPrice(new BigDecimal("29.90"));
        patch.setStock(0);
        System.out.println("MP按主键更新行数 = " + mapper.updateById(patch));
        System.out.println("MP更新后查询 = " + mapper.selectById(album.getAlbumId()));
        System.out.println("MP按主键删除行数 = " + mapper.deleteById(album.getAlbumId()));
        System.out.println("MP删除后查询 = " + mapper.selectById(album.getAlbumId()));
    }
}
