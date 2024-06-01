package me.croabeast.sir.plugin.logger;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class LoggerLine {
    final String line;
    final boolean usePrefix;
}
