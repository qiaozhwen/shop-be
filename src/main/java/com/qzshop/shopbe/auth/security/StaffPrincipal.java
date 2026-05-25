package com.qzshop.shopbe.auth.security;

import java.util.List;

public record StaffPrincipal(long staffId, List<String> roles) {}
