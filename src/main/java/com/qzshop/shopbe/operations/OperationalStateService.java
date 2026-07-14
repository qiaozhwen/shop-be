package com.qzshop.shopbe.operations;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OperationalStateService {

    private static final long STATE_ID = 1L;

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public OperationalStateService(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public Optional<State> load() {
        try {
            String payload = jdbc.queryForObject(
                    "select payload from operational_state where id = ?",
                    String.class,
                    STATE_ID);
            return Optional.of(objectMapper.readValue(payload, State.class));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("operational state is not valid JSON", ex);
        }
    }

    @Transactional
    public void save(State state) {
        try {
            String payload = objectMapper.writeValueAsString(state);
            int updated = jdbc.update(
                    "update operational_state set payload = ?, version = version + 1, updated_at = current_timestamp where id = ?",
                    payload,
                    STATE_ID);
            if (updated == 0) {
                jdbc.update(
                        "insert into operational_state (id, version, payload, updated_at) values (?, 1, ?, current_timestamp)",
                        STATE_ID,
                        payload);
            }
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("operational state cannot be serialized", ex);
        }
    }

    public record State(
            long nextId,
            List<Map<String, Object>> categories,
            List<Map<String, Object>> inventory,
            List<Map<String, Object>> orders,
            List<Map<String, Object>> processingTasks,
            List<Map<String, Object>> suppliers,
            List<Map<String, Object>> purchases,
            List<Map<String, Object>> losses,
            List<Map<String, Object>> members,
            List<Map<String, Object>> staff,
            List<Map<String, Object>> pricing) {
    }
}
