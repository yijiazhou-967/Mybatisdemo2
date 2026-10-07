package com.music.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 音乐专辑实体，与数据库表 music_store.album 一一对应。
 * 字段采用驼峰命名，配合全局配置 mapUnderscoreToCamelCase 自动映射下划线列。
 */
public class Album {

    /** 专辑编号（主键，自增） */
    private Integer albumId;
    /** 专辑名称 */
    private String title;
    /** 歌手 / 乐队 */
    private String artist;
    /** 音乐风格 */
    private String genre;
    /** 售价（元） */
    private BigDecimal price;
    /** 库存数量 */
    private Integer stock;
    /** 发行日期 */
    private LocalDate releaseDate;
    /** 状态：1 在售 / 0 下架 */
    private Integer status;

    /** 无参构造：MyBatis 结果映射实例化对象所必需 */
    public Album() {
    }

    /** 便捷构造：不含自增主键，便于快速创建待插入对象 */
    public Album(String title, String artist, String genre,
                 BigDecimal price, Integer stock, LocalDate releaseDate, Integer status) {
        this.title = title;
        this.artist = artist;
        this.genre = genre;
        this.price = price;
        this.stock = stock;
        this.releaseDate = releaseDate;
        this.status = status;
    }

    public Integer getAlbumId() {
        return albumId;
    }

    public void setAlbumId(Integer albumId) {
        this.albumId = albumId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Album{" +
                "albumId=" + albumId +
                ", title='" + title + '\'' +
                ", artist='" + artist + '\'' +
                ", genre='" + genre + '\'' +
                ", price=" + price +
                ", stock=" + stock +
                ", releaseDate=" + releaseDate +
                ", status=" + status +
                '}';
    }
}
