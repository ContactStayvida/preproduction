package com.stayvida.backend.repository;

import com.stayvida.backend.model.Feature;
import com.stayvida.backend.model.Amenity;
import com.stayvida.backend.model.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class LookupRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ---------- ADD ----------
    public void addFeature(String name) {
        jdbcTemplate.update(
                "INSERT INTO features (name, status) VALUES (?, 'enable')",
                name
        );
    }

    public void addAmenity(String name) {
        jdbcTemplate.update(
                "INSERT INTO amenities (name, status) VALUES (?, 'enable')",
                name
        );
    }

    public void addTag(String name) {
        jdbcTemplate.update(
                "INSERT INTO tags (name, status) VALUES (?, 'enable')",
                name
        );
    }

    // ---------- FETCH ----------
    public List<Feature> getFeatures() {
        return jdbcTemplate.query("SELECT * FROM features",
                (rs, rowNum) -> {
                    Feature f = new Feature();
                    f.setFeature_id(rs.getInt("feature_id"));
                    f.setName(rs.getString("name"));
                    f.setStatus(rs.getString("status"));
                    return f;
                });
    }

    public List<Amenity> getAmenities() {
        return jdbcTemplate.query("SELECT * FROM amenities",
                (rs, rowNum) -> {
                    Amenity a = new Amenity();
                    a.setAmenity_id(rs.getInt("amenity_id"));
                    a.setName(rs.getString("name"));
                    a.setStatus(rs.getString("status"));
                    return a;
                });
    }

    public List<Tag> getTags() {
        return jdbcTemplate.query("SELECT * FROM tags",
                (rs, rowNum) -> {
                    Tag t = new Tag();
                    t.setTag_id(rs.getInt("tag_id"));
                    t.setName(rs.getString("name"));
                    t.setStatus(rs.getString("status"));
                    return t;
                });
    }
}
