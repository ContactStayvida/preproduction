package com.stayvida.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.stayvida.backend.dto.UserListDTO;
import com.stayvida.backend.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserListDTO> getUserList() {
        return userRepository.fetchUserList();
    }
}
