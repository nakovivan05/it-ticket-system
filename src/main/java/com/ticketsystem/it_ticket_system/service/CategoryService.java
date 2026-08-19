package com.ticketsystem.it_ticket_system.service;

import com.ticketsystem.it_ticket_system.dto.CategoryDTO;
import com.ticketsystem.it_ticket_system.dto.CreateCategoryDTO;
import com.ticketsystem.it_ticket_system.dto.UpdateCategoryDTO;
import com.ticketsystem.it_ticket_system.exception.CategoryNotFoundException;
import com.ticketsystem.it_ticket_system.exception.DuplicateCategoryNameException;
import com.ticketsystem.it_ticket_system.exception.ValidationException;
import com.ticketsystem.it_ticket_system.model.Category;
import com.ticketsystem.it_ticket_system.model.EntityType;
import com.ticketsystem.it_ticket_system.model.Operation;
import com.ticketsystem.it_ticket_system.repository.CategoryRepository;
import com.ticketsystem.it_ticket_system.repository.TicketRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final TicketRepository ticketRepository;
    private final AuditLogService auditLogService;

    public CategoryService(CategoryRepository categoryRepository, TicketRepository ticketRepository, AuditLogService auditLogService) {
        this.categoryRepository = categoryRepository;
        this.ticketRepository = ticketRepository;
        this.auditLogService = auditLogService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE','TECHNICIAN')")
    public CategoryDTO getCategoryById(Long id) {

        return categoryRepository.findById(id)
                .map(CategoryDTO::fromEntity)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with id: " + id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE','TECHNICIAN')")
    public List<CategoryDTO> getAllCategories()
    {
        return categoryRepository.findAll().stream()
                .map(CategoryDTO::fromEntity)
                .toList();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE','TECHNICIAN')")
    public CategoryDTO getCategoryByName(String name) {

        return categoryRepository.findByName(name)
                .map(CategoryDTO::fromEntity)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with name: " + name));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryDTO createCategory(CreateCategoryDTO createCategoryDTO) {
        String currentUser = getCurrentUser();
        Category category = Category.builder()
                .name(createCategoryDTO.getName())
                .description(createCategoryDTO.getDescription())
                .build();
        if (categoryRepository.findByName(createCategoryDTO.getName()).isPresent()) {
            throw new DuplicateCategoryNameException("Category name already exists: " + createCategoryDTO.getName());
        }
        Category savedCategory = categoryRepository.save(category);
        auditLogService.auditLog(EntityType.CATEGORY, Operation.CREATE,"Category created: " + createCategoryDTO.getName(), savedCategory.getId(), getCurrentUser());
        return CategoryDTO.fromEntity(savedCategory);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryDTO updateCategory(Long id, UpdateCategoryDTO updateCategoryDTO) {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with id: " + id));

        if(updateCategoryDTO.getName() != null) {
            if(!existingCategory.getName().equals(updateCategoryDTO.getName()) &&
                    categoryRepository.findByName(updateCategoryDTO.getName()).isPresent()) {
                throw new DuplicateCategoryNameException("Category name already exists: " + updateCategoryDTO.getName());
            }
            existingCategory.setName(updateCategoryDTO.getName());
        }

        if(updateCategoryDTO.getDescription() != null) {
            existingCategory.setDescription(updateCategoryDTO.getDescription());
        }

        Category updatedCategory = categoryRepository.save(existingCategory);
        auditLogService.auditLog(EntityType.CATEGORY, Operation.UPDATE, "Category updated: " + existingCategory.getName(), updatedCategory.getId(), getCurrentUser());
        return CategoryDTO.fromEntity(updatedCategory);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteCategory(Long id)
    {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with id: " + id));
        if (ticketRepository.existsByCategoryId(id)) {
            throw new ValidationException("Cannot delete category that is referenced by tickets");
        }
        categoryRepository.delete(existingCategory);
        auditLogService.auditLog(EntityType.CATEGORY, Operation.DELETE, "Category deleted: " + existingCategory.getName(),id, getCurrentUser());
    }

    public String getCurrentUser() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

}