package com.mateirobescu.thesis.users;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser() {
        User newUser = User.builder().build();
        return userRepository.save(newUser);
    }

    public User getUserById(UUID id) {
        //TODO throw custom exception
        return userRepository.findById(id).orElseThrow(RuntimeException::new);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User patchUser(UUID id, UserPatchRequest request) {
        User userToPatch = this.getUserById(id);

        return userToPatch;
    }

    public void deleteUserById(UUID id) {
        User userToDelete = this.getUserById(id);
        userRepository.delete(userToDelete);
    }

}
