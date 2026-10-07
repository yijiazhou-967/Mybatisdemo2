package com.music;

import com.music.entity.Album;
import com.music.mapper.AlbumMapper;
import com.music.util.MyBatisUtil;
import org.apache.ibatis.session.SqlSession;

import java.util.List;

/**
 * 第一个 MyBatis 程序（任务二 步骤 2.4）。
 * 演示：读取配置 -> 构建 SqlSessionFactory（工具类封装）-> 开启会话 -> 获取 Mapper 代理 -> 执行查询。
 */
public class MusicStoreDemo {

    public static void main(String[] args) {
        // try-with-resources：会话用完自动关闭
        try (SqlSession session = MyBatisUtil.openSession()) {
            AlbumMapper mapper = session.getMapper(AlbumMapper.class);

            List<Album> albums = mapper.selectAll();
            System.out.println("专辑总数：" + albums.size());
            albums.forEach(System.out::println);
        }
    }
}
