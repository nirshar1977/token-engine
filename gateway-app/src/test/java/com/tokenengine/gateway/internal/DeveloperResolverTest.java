package com.tokenengine.gateway.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tokenengine.shared.api.DeveloperId;
import org.junit.jupiter.api.Test;

class DeveloperResolverTest {

    @Test
    void resolvesConfiguredKeys() {
        DeveloperResolver resolver = new DeveloperResolver(" nir:k1 , dana:k2", true);

        assertThat(resolver.resolve("k1")).contains(new DeveloperId("nir"));
        assertThat(resolver.resolve("k2")).contains(new DeveloperId("dana"));
        assertThat(resolver.resolve("nope")).isEmpty();
        assertThat(resolver.resolve(null)).isEmpty();
    }

    @Test
    void fallsBackToAnonymousWhenKeysAreOptional() {
        assertThat(new DeveloperResolver("", false).resolve(null)).contains(DeveloperId.ANONYMOUS);
    }

    @Test
    void rejectsMalformedOrDuplicateEntries() {
        assertThatThrownBy(() -> new DeveloperResolver("nir", true)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DeveloperResolver("a:k,b:k", true)).isInstanceOf(IllegalArgumentException.class);
    }
}
