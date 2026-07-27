package com.semd.backend.service;

import com.semd.backend.dto.MedicalHospitalDto;
import com.semd.backend.dto.MedicalHospitalRequest;
import com.semd.backend.entity.MedicalHospital;
import com.semd.backend.exception.BusinessConflictException;
import com.semd.backend.exception.ResourceNotFoundException;
import com.semd.backend.repository.MedicalHospitalRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MedicalHospitalService {

    private final MedicalHospitalRepository repository;
    private final GeometryFactory geometryFactory =
            new GeometryFactory(new PrecisionModel(), 4326);

    public MedicalHospitalService(MedicalHospitalRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<MedicalHospitalDto> search(String keyword, Boolean isActive, Pageable pageable) {
        Specification<MedicalHospital> specification = (root, query, cb) -> cb.conjunction();
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("hospitalName")), pattern),
                    cb.like(cb.lower(root.get("hospitalAddress")), pattern),
                    cb.like(cb.lower(root.get("contactPhone")), pattern)
            ));
        }
        if (isActive != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("isActive"), isActive));
        }
        return repository.findAll(specification, pageable).map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public MedicalHospitalDto getById(Integer id) {
        return repository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trung tâm cấp cứu với ID: " + id));
    }

    @Transactional
    public MedicalHospitalDto create(MedicalHospitalRequest request) {
        String hospitalName = request.hospitalName().trim();
        if (repository.existsByHospitalNameIgnoreCase(hospitalName)) {
            throw new BusinessConflictException(
                    "Tên bệnh viện đã tồn tại: " + hospitalName);
        }

        MedicalHospital hospital = new MedicalHospital();
        applyRequest(hospital, request);
        return mapToDto(repository.save(hospital));
    }

    @Transactional
    public MedicalHospitalDto update(Integer id, MedicalHospitalRequest request) {
        MedicalHospital hospital = findById(id);
        String hospitalName = request.hospitalName().trim();
        if (repository.existsByHospitalNameIgnoreCaseAndIdNot(hospitalName, id)) {
            throw new BusinessConflictException(
                    "Tên bệnh viện đã được sử dụng: " + hospitalName);
        }

        applyRequest(hospital, request);
        return mapToDto(repository.save(hospital));
    }

    @Transactional
    public void delete(Integer id) {
        MedicalHospital hospital = findById(id);
        try {
            repository.delete(hospital);
            repository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessConflictException(
                    "Không thể xóa bệnh viện đang được sử dụng bởi mission hoặc mô phỏng");
        }
    }

    private MedicalHospital findById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy trung tâm cấp cứu với ID: " + id));
    }

    private void applyRequest(
            MedicalHospital hospital,
            MedicalHospitalRequest request) {
        var location = geometryFactory.createPoint(
                new Coordinate(request.longitude(), request.latitude()));
        location.setSRID(4326);

        hospital.setHospitalName(request.hospitalName().trim());
        hospital.setHospitalAddress(trimToNull(request.hospitalAddress()));
        hospital.setLocation(location);
        hospital.setCapabilities(request.capabilities());
        hospital.setContactPhone(trimToNull(request.contactPhone()));
        hospital.setIsActive(request.isActive() != null ? request.isActive() : true);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private MedicalHospitalDto mapToDto(MedicalHospital hospital) {
        return new MedicalHospitalDto(
                hospital.getId(),
                hospital.getHospitalName(),
                hospital.getHospitalAddress(),
                hospital.getLocation() != null ? hospital.getLocation().getX() : null,
                hospital.getLocation() != null ? hospital.getLocation().getY() : null,
                hospital.getCapabilities(),
                hospital.getContactPhone(),
                hospital.getIsActive()
        );
    }
}
