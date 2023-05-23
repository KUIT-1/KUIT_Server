package kuit.server.service;

import kuit.server.common.exception.UserException;
import kuit.server.dao.UserDao;
import kuit.server.dto.user.PostLoginRequest;
import kuit.server.dto.user.PostLoginResponse;
import kuit.server.dto.user.PostUserRequest;
import kuit.server.dto.user.PostUserResponse;
import kuit.server.util.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static kuit.server.common.response.status.BaseExceptionResponseStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public PostUserResponse signUp(PostUserRequest postUserRequest) {
        log.info("[UserService.createUser]");

        // TODO: 1. validation (중복 검사)
        if (userDao.hasDuplicateEmail(postUserRequest.getEmail())) {
            throw new UserException(DUPLICATE_EMAIL);
        }
        if (userDao.hasDuplicateNickName(postUserRequest.getNickname())) {
            throw new UserException(DUPLICATE_NICKNAME);
        }

        // TODO: 2. password 암호화
        String encodedPassword = passwordEncoder.encode(postUserRequest.getPassword());
        postUserRequest.resetPassword(encodedPassword);

        // TODO: 3. DB insert & userId 반환
        long userId = userDao.createUser(postUserRequest);

        // TODO: 4. JWT 토큰 생성
        String jwt = jwtTokenProvider.createToken(postUserRequest.getEmail(), userId);

        return new PostUserResponse(userId, jwt);
    }

    public long findUserIdByEmail(String email) {
        return userDao.findUserIdByEmail(email);
    }

    public PostLoginResponse login(PostLoginRequest postLoginRequest, long userId) {
        log.info("[UserService.login]");

        // TODO: 1. 비밀번호 일치 확인
        validatePassword(postLoginRequest.getPassword(), userId);

        // TODO: 2. JWT 갱신
        String updatedJwt = jwtTokenProvider.createToken(postLoginRequest.getEmail(), userId);

        return new PostLoginResponse(userId, updatedJwt);
    }

    private void validatePassword(String password, long userId) {
        String encodedPassword = userDao.getPasswordByUserId(userId);
        if (!passwordEncoder.matches(password, encodedPassword)) {
            throw new UserException(PASSWORD_NO_MATCH);
        }
    }

}
