package com.music.mapper;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.music.entity.Album;
import com.music.query.AlbumQueries;
import com.music.util.MyBatisUtil;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/** 无数据库测试：确认 MP SQL 注入、实体注解与真正生效的分页配置。 */
class AlbumPlusMapperConfigTest {
    private final Configuration configuration = MyBatisUtil.getSqlSessionFactory().getConfiguration();

    @Test
    void configuration_通用SQL与原XML映射共存() {
        assertTrue(configuration instanceof MybatisConfiguration);
        for (String method : Arrays.asList("insert", "selectById", "updateById", "deleteById", "selectList", "selectCount")) {
            assertTrue(configuration.hasStatement("com.music.mapper.AlbumPlusMapper." + method), method);
        }
        assertTrue(configuration.hasStatement("com.music.mapper.AlbumMapper.updateSelectiveRows"));
        assertTrue(configuration.hasMapper(AlbumPlusMapper.class));
    }

    @Test
    void pagination_已注册MySQL分页且保持页码溢出为空() {
        assertEquals(1, configuration.getInterceptors().size());
        MybatisPlusInterceptor outer = (MybatisPlusInterceptor) configuration.getInterceptors().get(0);
        assertEquals(1, outer.getInterceptors().size());
        PaginationInnerInterceptor pagination = (PaginationInnerInterceptor) outer.getInterceptors().get(0);
        assertEquals(DbType.MYSQL, pagination.getDbType());
        assertFalse(pagination.isOverflow());
        assertEquals(Long.valueOf(100), pagination.getMaxLimit());
    }

    @Test
    void entity_自增主键日期映射与状态含义正确() {
        TableInfo table = TableInfoHelper.getTableInfo(Album.class);
        assertNotNull(table);
        assertEquals("album", table.getTableName());
        assertEquals("album_id", table.getKeyColumn());
        assertEquals(IdType.AUTO, table.getIdType());
        assertFalse(table.isWithLogicDelete());
        assertTrue(table.getFieldList().stream().anyMatch(field ->
                "releaseDate".equals(field.getProperty()) && "release_date".equals(field.getColumn())));
    }

    @Test
    void wrappers_空白条件被忽略且输入使用绑定参数() {
        String blank = AlbumQueries.queryWrapper(" ", "\t", null).getSqlSegment();
        assertFalse(blank.contains("title LIKE"));
        assertFalse(blank.contains("genre ="));
        assertFalse(blank.contains("price >="));
        assertTrue(blank.contains("status ="));
        assertTrue(blank.contains("ORDER BY price DESC,album_id ASC"));
        String input = "' OR 1=1 --";
        var query = AlbumQueries.queryWrapper(input, "流行", new BigDecimal("30.00"));
        var lambda = AlbumQueries.lambdaQueryWrapper(input, "流行", new BigDecimal("30.00"));
        assertEquals(query.getSqlSegment(), lambda.getSqlSegment());
        assertFalse(query.getSqlSegment().contains(input));
        assertEquals(query.getParamNameValuePairs(), lambda.getParamNameValuePairs());
    }
}
