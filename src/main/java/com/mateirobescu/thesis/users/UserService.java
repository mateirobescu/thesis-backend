package com.mateirobescu.thesis.users;

import com.mateirobescu.thesis.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class UserService {
    UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser() {
        User newUser = User.builder().build();
        User savedUser = userRepository.save(newUser);
        //TODO maybe error handling here?
        log.info("User created id={}", savedUser.getId());
        return savedUser;
    }

    public User getUserById(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User", id));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User patchUser(UUID id, UserPatchRequest request) {
        User userToPatch = this.getUserById(id);
        log.info("User patched id={}", userToPatch.getId());
        return userToPatch;
    }

    public void deleteUserById(UUID id) {
        User userToDelete = this.getUserById(id);
        log.info("User deleting id={}", id);
        //TODO maybe error handling here?
        userRepository.delete(userToDelete);
        log.info("User deleted id={}", id);
    }

}
