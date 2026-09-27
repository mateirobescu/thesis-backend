package com.mateirobescu.thesis.users;

import com.mateirobescu.thesis.auth.AuthService;
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

    public User createUser(UserCreateCommand command) {
        User newUser = User.builder()
                .email(command.email())
                .passwordHash(command.hashedPassword())
                .firstName(command.firstName())
                .lastName(command.lastName())
                .build();

        User savedUser = userRepository.save(newUser);
        //TODO maybe error handling here?
        log.info("User created id={}", savedUser.getId());
        return savedUser;
    }

    public User getUserById(UUID id) {
        return userRepository.findByIdAndDeletedAtIsNull(id).orElseThrow(() -> new NotFoundException("User", id));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email).orElseThrow(() -> new NotFoundException("User", email));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User patchUser(UUID id, UserPatchCommand command) {
        User userToPatch = this.getUserById(id);
        User patchedUser = userRepository.save(userToPatch.patch(command));
        log.info("User patched id={}", userToPatch.getId());
        return patchedUser;
    }

    public void deleteUserById(UUID id) {
        User userToDelete = this.getUserById(id);
        //TODO maybe error handling here?
        userToDelete.markDeleted();
        userRepository.save(userToDelete);
        log.info("User marked deleted id={}", id);
    }

}
