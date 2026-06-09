package com.ticketsystem.it_ticket_system.service;

import com.ticketsystem.it_ticket_system.dto.CategoryDTO;
import com.ticketsystem.it_ticket_system.exception.CategoryNotFoundException;
import com.ticketsystem.it_ticket_system.exception.DuplicateCategoryNameException;
import com.ticketsystem.it_ticket_system.model.Category;
import com.ticketsystem.it_ticket_system.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
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

        if (!existingCategory.getName().equals(categoryDTO.getName()) &&
            categoryRepository.findByName(categoryDTO.getName()).isPresent()) {
            throw new DuplicateCategoryNameException("Category name already exists: " + categoryDTO.getName());
        }

        existingCategory.setName(categoryDTO.getName());
        existingCategory.setDescription(categoryDTO.getDescription());
        Category updatedCategory = categoryRepository.save(existingCategory);
        return CategoryDTO.fromEntity(updatedCategory);
    }

    @Transactional
    public void deleteCategory(Long id)
    {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with id: " + id));
        categoryRepository.delete(existingCategory);
    }

}
