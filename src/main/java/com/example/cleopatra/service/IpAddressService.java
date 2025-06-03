package com.example.cleopatra.service;


import jakarta.servlet.http.HttpServletRequest;

public interface IpAddressService {

    /**
     * Получение реального IP адреса клиента
     * @param request HTTP запрос
     * @return IP адрес клиента
     */
    String getClientIpAddress(HttpServletRequest request);

    /**
     * Получение User-Agent из запроса
     * @param request HTTP запрос
     * @return User-Agent или "Unknown" если не найден
     */
    String getUserAgent(HttpServletRequest request);

    /**
     * Записать визит пользователя
     * @param visitedUserId ID посещаемого пользователя
     * @param currentUserId ID текущего пользователя
     * @param request HTTP запрос для получения метаданных
     */
    void recordUserVisit(Long visitedUserId, Long currentUserId, HttpServletRequest request);
}
