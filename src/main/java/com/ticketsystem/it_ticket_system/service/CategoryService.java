package com.ticketsystem.it_ticket_system.service;

import com.ticketsystem.it_ticket_system.dto.CategoryDTO;
import com.ticketsystem.it_ticket_system.exception.CategoryNotFoundException;
import com.ticketsystem.it_ticket_system.exception.DuplicateCategoryNameException;
import com.ticketsystem.it_ticket_system.exception.ValidationException;
import com.ticketsystem.it_ticket_system.model.Category;
import com.ticketsystem.it_ticket_system.repository.CategoryRepository;
import com.ticketsystem.it_ticket_system.repository.TicketRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final TicketRepository ticketRepository;

    public CategoryService(CategoryRepository categoryRepository, TicketRepository ticketRepository) {
        this.categoryRepository = categoryRepository;
        this.ticketRepository = ticketRepository;
    }

    private Category toEntity(CategoryDTO categoryDTO)
    {
        return Category.builder()
                .id(categoryDTO.getId())
                .name(categoryDTO.getName())
                .description(categoryDTO.getDescription())
                .build();
    }

    public CategoryDTO getCategoryById(Long id) {

        return categoryRepository.findById(id)
                .map(CategoryDTO::fromEntity)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with id: " + id));
    }

    public List<CategoryDTO> getAllCategories()
    {
        return categoryRepository.findAll().stream()
                .map(CategoryDTO::fromEntity)
                .toList();
    }

    public CategoryDTO getCategoryByName(String name) {

        return categoryRepository.findByName(name)
                .map(CategoryDTO::fromEntity)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with name: " + name));
    }

    @Transactional
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        if (categoryDTO.getName() == null || categoryDTO.getName().trim().isEmpty()) {
            throw new ValidationException("Category name is required");
        }

        if (categoryRepository.findByName(categoryDTO.getName()).isPresent()) {
            throw new DuplicateCategoryNameException("Category name already exists: " + categoryDTO.getName());
        }
        Category category = toEntity(categoryDTO);
        Category savedCategory = categoryRepository.save(category);
        return CategoryDTO.fromEntity(savedCategory);
    }

    @Transactional
    public CategoryDTO updateCategory(Long id, CategoryDTO categoryDTO) {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with id: " + id));

        if (categoryDTO.getName() != null) {
            if (categoryDTO.getName().trim().isEmpty()) {
                throw new ValidationException("Category name cannot be empty");
            }
            if (!existingCategory.getName().equals(categoryDTO.getName()) &&
                    categoryRepository.findByName(categoryDTO.getName()).isPresent()) {
                throw new DuplicateCategoryNameException("Category name already exists: " + categoryDTO.getName());
            }
        }

        if (categoryDTO.getName() != null) {
            existingCategory.setName(categoryDTO.getName());
        }

        if(categoryDTO.getDescription()!=null)
        {
            existingCategory.setDescription(categoryDTO.getDescription());
        }
        Category updatedCategory = categoryRepository.save(existingCategory);
        return CategoryDTO.fromEntity(updatedCategory);
    }

    @Transactional
    public void deleteCategory(Long id)
    {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with id: " + id));
        if (ticketRepository.existsByCategoryId(id)) {
            throw new ValidationException("Cannot delete category that is referenced by tickets");
        }
        categoryRepository.delete(existingCategory);
    }

}