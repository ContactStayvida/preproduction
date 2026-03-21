package com.stayvida.backend.repository;

import com.stayvida.backend.dto.Ad;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AdRepository {

    private final JdbcTemplate jdbcTemplate;

    public AdRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void disableAllActiveAds() {

        String sql = "UPDATE ads SET is_active = false WHERE is_active = true";

        jdbcTemplate.update(sql);
    }

    public void createAd(Ad ad) {

        String sql = """
                    INSERT INTO ads (ad_id, banner_image, hotel_id, click_count, is_active)
                    VALUES (?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(sql,
                ad.getAdId(),
                ad.getBannerImage(),
                ad.getHotelId(),
                ad.getClickCount(),
                ad.isActive());
    }

    public List<Ad> getAllAds() {

        String sql = "SELECT * FROM ads";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Ad ad = new Ad();
            ad.setAdId(rs.getString("ad_id"));
            ad.setBannerImage(rs.getString("banner_image"));
            ad.setHotelId(rs.getString("hotel_id"));
            ad.setClickCount(rs.getInt("click_count"));
            ad.setActive(rs.getBoolean("is_active"));
            ad.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            ad.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            return ad;
        });
    }

    public Ad getCurrentAd() {

        String sql = """
                    SELECT * FROM ads
                    WHERE is_active = true
                    LIMIT 1
                """;

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            Ad ad = new Ad();
            ad.setAdId(rs.getString("ad_id"));
            ad.setBannerImage(rs.getString("banner_image"));
            ad.setHotelId(rs.getString("hotel_id"));
            ad.setActive(rs.getBoolean("is_active"));
            ad.setClickCount(rs.getInt("click_count"));
            ad.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            ad.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            return ad;
        });
    }

    public void deleteAd(String adId) {

        String sql = "DELETE FROM ads WHERE ad_id = ?";

        jdbcTemplate.update(sql, adId);
    }

    public void setAdActive(String adId, boolean active) {

        String sql = "UPDATE ads SET is_active=? WHERE ad_id=?";

        jdbcTemplate.update(sql, active, adId);
    }

    public void incrementClickCount(String adId) {

        String sql = """
                    UPDATE ads
                    SET click_count = click_count + 1
                    WHERE ad_id = ?
                """;

        jdbcTemplate.update(sql, adId);
    }
}