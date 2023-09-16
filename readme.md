# kirok
프론트엔트 로직 라이브러리
## kirok를 왜 사용해야 하나요?
**로직을 공유**할 수 있고, **프론트엔드-독립적**으로 로직을 작성할 수 있습니다. wasm으로 **빠르고 안전한** 프론트엔드를 만들 수 있습니다.
## 예시
### React
- 본 예제: [kirok-react-example](https://github.com/devngho/kirok-react-example)
- 사용할 바인딩: [kirok-react-binding](https://github.com/devngho/kirok-react-binding)
```kt
// index.kt
@Serializable
@Model
data class Index(var count: Int = 0) // 사용할 데이터를 정의하고...

@Init
fun init(): Index = Index() // 초기 데이터를 정의하고...

@Intent
fun add(model: Index) { // 로직을 작성합니다.
    model.count += 1 // 자동으로 모델을 업데이트합니다. 모델의 불변성을 보장합니다.
}
```
``` jsx
// index.jsx
import { useSimple } from './kirok/Simple'

function Counter() {
    const [v, { increment }] = useSimple() // index 모델을 사용할 수 있는 훅입니다.
    return (
        <div id="index">{v.count}</div>
        <button onClick={increment}>+</button>
    )
}

export default function App() {
    return (
        <Counter />
    )
}
```
### Svelte
- 본 예제: [kirok-svelte-example](https://github.com/devngho/kirok-svelte-example)
- 사용할 바인딩: [kirok-svelte-binding](https://github.com/devngho/kirok-svelte-binding)
```kt
// index.kt
// 위의 index.kt와 동일한 코드를 사용하겠습니다.
```
``` html
<!-- App.svelte -->
<script lang="ts">
  import { useSimple } from "./kirok/Simple";
  
  const [simpleCounter, { increment, incrementSome }] = useSimple()
</script>

<div>
    <button on:click={() => increment()}>{$simpleCounter.count}</button>
</div>
```
## 성능
[이 파일](performance.md)을 참조하세요.
## Behind the sence
kirok은 ksp를 사용해 Model과 Intent가 동작하도록 코드를 생성하고, 바인딩을 생성합니다.