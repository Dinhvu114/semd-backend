package com.semd.backend.service;

import com.semd.backend.dto.CoordinateDto;
import com.semd.backend.dto.OperationZoneDto;
import com.semd.backend.dto.OperationZoneRequest;
import com.semd.backend.entity.OperationZone;
import com.semd.backend.exception.ResourceNotFoundException;
import com.semd.backend.repository.OperationZoneRepository;
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
public class OperationZoneService {

    private final OperationZoneRepository repository;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public OperationZoneService(OperationZoneRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public OperationZoneDto createOperationZone(OperationZoneRequest request) {
        if (repository.existsByZoneName(request.zoneName())) {
            throw new IllegalArgumentException("Tên vùng quản lý '" + request.zoneName() + "' đã tồn tại");
        }

        OperationZone operationZone = new OperationZone();
        operationZone.setZoneName(request.zoneName());
        operationZone.setCoverageArea(buildPolygon(request.coverageArea()));
        operationZone.setIsActive(request.isActive() != null ? request.isActive() : true);
        operationZone.setCreatedAt(LocalDateTime.now());

        OperationZone saved = repository.save(operationZone);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<OperationZoneDto> getAllOperationZones() {
        return repository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OperationZoneDto getOperationZoneById(Integer id) {
        OperationZone operationZone = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vùng quản lý với ID: " + id));
        return mapToDto(operationZone);
    }

    @Transactional
    public OperationZoneDto updateOperationZone(Integer id, OperationZoneRequest request) {
        OperationZone operationZone = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vùng quản lý với ID: " + id));

        if (repository.existsByZoneNameAndIdNot(request.zoneName(), id)) {
            throw new IllegalArgumentException("Tên vùng quản lý '" + request.zoneName() + "' đã được sử dụng bởi vùng quản lý khác");
        }

        operationZone.setZoneName(request.zoneName());
        operationZone.setCoverageArea(buildPolygon(request.coverageArea()));
        if (request.isActive() != null) {
            operationZone.setIsActive(request.isActive());
        }

        OperationZone updated = repository.save(operationZone);
        return mapToDto(updated);
    }

    @Transactional
    public void deleteOperationZone(Integer id) {
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

    private OperationZoneDto mapToDto(OperationZone operationZone) {
        List<CoordinateDto> coords = null;
        if (operationZone.getCoverageArea() != null) {
            coords = java.util.Arrays.stream(operationZone.getCoverageArea().getCoordinates())
                    .map(c -> new CoordinateDto(c.x, c.y))
                    .collect(Collectors.toList());
        }

        return new OperationZoneDto(
                operationZone.getId(),
                operationZone.getZoneName(),
                coords,
                operationZone.getIsActive(),
                operationZone.getCreatedAt()
        );
    }
}
