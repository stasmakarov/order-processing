package com.company.orderprocessing.security;

import io.jmix.security.role.annotation.ResourceRole;
import io.jmix.securityflowui.role.annotation.ViewPolicy;

@ResourceRole(name = "AnonymousAccessRole", code = AnonymousAccessRole.CODE)
public interface AnonymousAccessRole {
    String CODE = "anonymous-access-role";

    @ViewPolicy(viewIds = {"ord_MainView"})
    void allowMainViewAccess();

    @ViewPolicy(viewIds = {"ord_DemoView"})
    void allowDemoViewAccess();
}