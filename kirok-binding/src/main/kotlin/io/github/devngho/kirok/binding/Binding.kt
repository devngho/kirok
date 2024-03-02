package io.github.devngho.kirok.binding

import java.nio.file.Path

interface Binding {
    /**
     * JavaScript 등으로 바인딩을 생성합니다.
     * @param buildDir 생성할 디렉토리
     * @param models 생성할 모델
     */
    suspend fun create(buildDir: Path, models: List<BindingModel>)
}