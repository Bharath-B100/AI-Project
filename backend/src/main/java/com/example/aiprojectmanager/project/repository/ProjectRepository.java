package com.example.aiprojectmanager.project.repository;
import com.example.aiprojectmanager.project.domain.Project; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface ProjectRepository extends JpaRepository<Project,Long> { List<Project> findAllByOwnerIdOrderByUpdatedAtDesc(Long ownerId); Optional<Project> findByIdAndOwnerId(Long id, Long ownerId); }
