/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014-2017 ForgeRock AS. All Rights Reserved
 */

package org.forgerock.openam.examples;

import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.ScopeValidator;
import org.forgerock.oauth2.core.Token;
import org.forgerock.oauth2.core.UserInfoClaims;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import java.util.regex.Pattern;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * Custom scope validators implement the
 * {@link org.forgerock.oauth2.core.ScopeValidator} interface.
 *
 * <p>
 * This example sets read and write permissions according to the scopes set.
 * </p>
 *
 * <ul>
 *
 * <li>The {@code validateAuthorizationScope} method adds default scopes, or any
 * allowed scopes provided.</li>
 *
 * <li>The {@code validateAccessTokenScope} method adds default scopes, or any
 * allowed scopes provided.</li>
 *
 * <li>The {@code validateRefreshTokenScope} method adds the scopes from the
 * access token, or any requested scopes provided that are also in the access
 * token scopes.</li>
 *
 * <li>The {@code getUserInfo} method populates scope values and sets the
 * resource owner ID to return.</li>
 *
 * <li>The {@code evaluateScope} method populates scope values to return.</li>
 *
 * <li>The {@code additionalDataToReturnFromAuthorizeEndpoint} method returns no
 * additional data (an empty Map).</li>
 *
 * <li>The {@code additionalDataToReturnFromTokenEndpoint} method adds no
 * additional data.</li>
 *
 * </ul>
 */
public class ParameterizedScopeValidator implements ScopeValidator {

    // Assumption: List of allowed scopes is pairwise unmatchable

    static HashMap<String, Pattern> regExPatterns = new HashMap<String, Pattern>(); 


    private List<String> filter(List<String> reqestedScopes, String regex) {
        Pattern pattern = null;
        
        if (regExPatterns.containsKey(regex)) {
            pattern = regExPatterns.get(regex);
        } else {
            pattern = Pattern.compile(regex);
            regExPatterns.put(regex, pattern);
        }
        
        List<String> matching = reqestedScopes.stream().filter(pattern.asPredicate()).collect(Collectors.toList());
        System.out.println(matching.toString());
        return matching;
    }

    @Override
    public Set<String> validateAuthorizationScope(ClientRegistration clientRegistration, Set<String> scope,
            OAuth2Request oAuth2Request) {
        if (scope == null || scope.isEmpty()) {
            return clientRegistration.getDefaultScopes();
        }

        List<String> requestedScopes = new ArrayList(scope);
        List<String> authorizedScopes = new ArrayList();

        List<String> allowedScopes = new ArrayList(clientRegistration.getAllowedScopes());
        Iterator it = allowedScopes.iterator();
        while (it.hasNext()) {
            authorizedScopes.addAll(filter(requestedScopes, (String)it.next()));
        }
        // scopes.retainAll(scope);
        return new HashSet<>(authorizedScopes);
    }

    @Override
    public Set<String> validateAccessTokenScope(ClientRegistration clientRegistration, Set<String> scope,
            OAuth2Request request) {
        System.out.println("Steffo: in validateAT.");
        if (scope == null || scope.isEmpty()) {
            return clientRegistration.getDefaultScopes();
        }

        List<String> requestedScopes = new ArrayList(scope);
        List<String> authorizedScopes = new ArrayList();

        List<String> allowedScopes = new ArrayList(clientRegistration.getAllowedScopes());
        System.out.println("Steffo: in validateAT. AllowedScopes: " + allowedScopes.toString());
        Iterator it = allowedScopes.iterator();
        while (it.hasNext()) {
            authorizedScopes.addAll(filter(requestedScopes, (String)it.next()));
        }
        // scopes.retainAll(scope);
        return new HashSet<>(authorizedScopes);
    }

    @Override
    public Set<String> validateRefreshTokenScope(ClientRegistration clientRegistration, Set<String> requestedScope,
            Set<String> tokenScope, OAuth2Request request) {
        if (requestedScope == null || requestedScope.isEmpty()) {
            return tokenScope;
        }

        Set<String> scopes = new HashSet<String>(tokenScope);
        scopes.retainAll(requestedScope);
        return scopes;
    }

    /**
     * Set read and write permissions according to scope.
     *
     * @param token The access token presented for validation.
     * @return The map of read and write permissions, with permissions set to
     *         {@code true} or {@code false}, as appropriate.
     */
    private Map<String, Object> mapScopes(AccessToken token) {
        Set<String> scopes = token.getScope();
        Map<String, Object> map = new HashMap<String, Object>();
        final String[] permissions = { "read", "write" };

        for (String scope : permissions) {
            if (scopes.contains(scope)) {
                map.put(scope, true);
            } else {
                map.put(scope, false);
            }
        }
        return map;
    }

    @Override
    public UserInfoClaims getUserInfo(ClientRegistration clientRegistration, AccessToken token, OAuth2Request request)
            throws UnauthorizedClientException {
        Map<String, Object> response = mapScopes(token);
        response.put("sub", token.getResourceOwnerId());
        UserInfoClaims userInfoClaims = new UserInfoClaims(response, null);
        return userInfoClaims;
    }

    @Override
    public Map<String, Object> evaluateScope(AccessToken token) {
        return mapScopes(token);
    }

    @Override
    public Map<String, String> additionalDataToReturnFromAuthorizeEndpoint(Map<String, Token> tokens,
            OAuth2Request request) {
        return new HashMap<String, String>(); // No special handling
    }

    @Override
    public void additionalDataToReturnFromTokenEndpoint(AccessToken token, OAuth2Request request)
            throws ServerException, InvalidClientException {
        // No special handling
    }
}
