package com.stayvida.backend.repository;

import com.stayvida.backend.model.Feature;
import com.stayvida.backend.model.Amenity;
import com.stayvida.backend.model.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class LookupRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // // ---------- ADD ----------
    // public void addFeature(String name) {
    // jdbcTemplate.update(
    // "INSERT INTO features (name, status) VALUES (?, 'enable')",
    // name
    // );
    // }

    // public void addAmenity(String name) {
    // jdbcTemplate.update(
    // "INSERT INTO amenities (name, status) VALUES (?, 'enable')",
    // name
    // );
    // }

    // public void addTag(String name) {
    // jdbcTemplate.update(
    // "INSERT INTO tags (name, status) VALUES (?, 'enable')",
    // name
    // );
    // }
    public Map<String, List<String>> addFeatures(List<String> names) {
        return addItems(names, "features");
    }

    public Map<String, List<String>> addAmenities(List<String> names) {
        return addItems(names, "amenities");
    }

    public Map<String, List<String>> addTags(List<String> names) {
        return addItems(names, "tags");
    }

    // ----------duplicte
    public Map<String, List<String>> addItems(
            List<String> names,
            String tableName) {

        // 1️⃣ Find existing
        String selectSql = "SELECT name FROM " + tableName + " WHERE name IN (:names)";

        NamedParameterJdbcTemplate namedJdbc = new NamedParameterJdbcTemplate(jdbcTemplate);

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("names", names);

        List<String> existing = namedJdbc.queryForList(
                selectSql,
                params,
                String.class);

        // 2️⃣ Filter new ones
        List<String> newItems = names.stream()
                .filter(name -> !existing.contains(name))
                .toList();

        // 3️⃣ Insert only new ones
        if (!newItems.isEmpty()) {
            String insertSql = "INSERT INTO " + tableName + " (name, status) VALUES (?, 'enable')";

            jdbcTemplate.batchUpdate(insertSql, newItems, newItems.size(),
                    (ps, name) -> ps.setString(1, name));
        }

        Map<String, List<String>> result = new HashMap<>();
        result.put("inserted", newItems);
        result.put("duplicates", existing);

        return result;
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
