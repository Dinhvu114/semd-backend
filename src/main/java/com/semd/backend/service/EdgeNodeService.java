package com.semd.backend.service;

import com.semd.backend.dto.CoordinateDto;
import com.semd.backend.dto.EdgeNodeDto;
import com.semd.backend.dto.EdgeNodeRequest;
import com.semd.backend.entity.EdgeNode;
import com.semd.backend.exception.ResourceNotFoundException;
import com.semd.backend.repository.EdgeNodeRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EdgeNodeService {

    private final EdgeNodeRepository repository;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public EdgeNodeService(EdgeNodeRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public EdgeNodeDto createEdgeNode(EdgeNodeRequest request) {
        if (repository.existsByNodeName(request.nodeName())) {
            throw new IllegalArgumentException("Tên vùng quản lý '" + request.nodeName() + "' đã tồn tại");
        }

        EdgeNode edgeNode = new EdgeNode();
        edgeNode.setNodeName(request.nodeName());
        edgeNode.setCoverageArea(buildPolygon(request.coverageArea()));
        edgeNode.setIsActive(request.isActive() != null ? request.isActive() : true);
        edgeNode.setCreatedAt(LocalDateTime.now());

        EdgeNode saved = repository.save(edgeNode);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<EdgeNodeDto> getAllEdgeNodes() {
        return repository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EdgeNodeDto getEdgeNodeById(Integer id) {
        EdgeNode edgeNode = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vùng quản lý với ID: " + id));
        return mapToDto(edgeNode);
    }

    @Transactional
    public EdgeNodeDto updateEdgeNode(Integer id, EdgeNodeRequest request) {
        EdgeNode edgeNode = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vùng quản lý với ID: " + id));

        if (repository.existsByNodeNameAndIdNot(request.nodeName(), id)) {
            throw new IllegalArgumentException("Tên vùng quản lý '" + request.nodeName() + "' đã được sử dụng bởi vùng quản lý khác");
        }

        edgeNode.setNodeName(request.nodeName());
        edgeNode.setCoverageArea(buildPolygon(request.coverageArea()));
        if (request.isActive() != null) {
            edgeNode.setIsActive(request.isActive());
        }

        EdgeNode updated = repository.save(edgeNode);
        return mapToDto(updated);
    }

    @Transactional
    public void deleteEdgeNode(Integer id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy vùng quản lý với ID: " + id);
        }
        repository.deleteById(id);
    }

    private Polygon buildPolygon(List<CoordinateDto> coordinates) {
        if (coordinates == null || coordinates.isEmpty()) {
            return null;
        }

        List<CoordinateDto> list = new java.util.ArrayList<>(coordinates);
        CoordinateDto first = list.get(0);
        CoordinateDto last = list.get(list.size() - 1);
        if (!first.longitude().equals(last.longitude()) || !first.latitude().equals(last.latitude())) {
            list.add(first);
        }

        if (list.size() < 4) {
            throw new IllegalArgumentException("Vùng bao (polygon) phải chứa ít nhất 3 tọa độ phân biệt (và tối thiểu 4 điểm để tạo vòng khép kín)");
        }

        Coordinate[] jtsCoords = new Coordinate[list.size()];
        for (int i = 0; i < list.size(); i++) {
            CoordinateDto dto = list.get(i);
            jtsCoords[i] = new Coordinate(dto.longitude(), dto.latitude());
        }

        LinearRing shell = geometryFactory.createLinearRing(jtsCoords);
        return geometryFactory.createPolygon(shell, null);
    }

    private EdgeNodeDto mapToDto(EdgeNode edgeNode) {
        List<CoordinateDto> coords = null;
        if (edgeNode.getCoverageArea() != null) {
            coords = java.util.Arrays.stream(edgeNode.getCoverageArea().getCoordinates())
                    .map(c -> new CoordinateDto(c.x, c.y))
                    .collect(Collectors.toList());
        }

        return new EdgeNodeDto(
                edgeNode.getId(),
                edgeNode.getNodeName(),
                coords,
                edgeNode.getIsActive(),
                edgeNode.getCreatedAt()
        );
    }
}
