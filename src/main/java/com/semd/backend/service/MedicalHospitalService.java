package com.semd.backend.service;

import com.semd.backend.dto.MedicalHospitalDto;
import com.semd.backend.entity.MedicalHospital;
import com.semd.backend.exception.ResourceNotFoundException;
import com.semd.backend.repository.MedicalHospitalRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MedicalHospitalService {

    private final MedicalHospitalRepository repository;

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

    private MedicalHospitalDto mapToDto(MedicalHospital hospital) {
        return new MedicalHospitalDto(
                hospital.getId(),
                hospital.getOwner() != null ? hospital.getOwner().getId() : null,
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
