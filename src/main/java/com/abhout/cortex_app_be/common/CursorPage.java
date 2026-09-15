package com.abhout.cortex_app_be.common;

import java.util.List;

public record CursorPage<T>(List<T> items, String nextCursor) {
}
