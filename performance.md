# 성능

kirok는 Wasm으로 컴파일되어 JS보다 뛰어난 성능을 제공하지만, 항상은 아닙니다.
성능을 측정해 봅시다.

## 테스트 환경
- 브라우저: Chrome 117.0.5938.89, Firefox 117.0.1
- OS: Windows 11
- CPU: AMD Ryzen 9 5950X
- RAM: 64GB
- 비교군: 브라우저 별로 JavaScript, kirok, kirok + Binaryen

## Fibonacci
5부터 50까지 피보나치 수를 계산합니다.
```kotlin
@Serializable
@Model
data class Fibonacci(var n: Int, var result: Int)

@Init
fun initFibonacci(): Fibonacci = Fibonacci(0, 0)

@Intent
fun calculate(fibonacci: Fibonacci) {
    val n = fibonacci.n
    fibonacci.result = if (n <= 1) {
        n
    } else {
        getFibonacci(n - 1) + getFibonacci(n - 2)
    }
}

fun getFibonacci(n: Int): Int {
    return if (n <= 1) {
        n
    } else {
        getFibonacci(n - 1) + getFibonacci(n - 2)
    }
}
```

```html
<!-- App.svelte -->
<script lang="ts">
    import { onMount } from "svelte";
    import { useFibonacci } from "./kirok/Fibonacci";

    const [fibonacci, { calculate }] = useFibonacci();
    function jsFibonacci(n: number): number {
        if (n < 2) return n;
        return jsFibonacci(n - 1) + jsFibonacci(n - 2);
    }

    onMount(() => {
        for (let i = 5; i <= 50; i += 5) {
            console.log(`n: ${i}`);
            
            let start = performance.now();
            let result = jsFibonacci(i);
            let end = performance.now();
            console.log(`js: ${end - start}ms`);
            console.log(result);
            
            start = performance.now();
            $fibonacci.n = i;
            calculate();
            end = performance.now();
            console.log(`kirok: ${end - start}ms`);
            console.log($fibonacci.result);
        }
    })
</script>
```

### 테스트 결과
낮은 숫자가 더 빠릅니다. 5부터 50까지 5씩 증가시키며 테스트했으며 5~25는 1ms 이하이므로 생략했습니다.
#### Chrome

| n     | JavaScript(s) | kirok(s) | kirok + Binaryen(s) |
|-------|---------------|----------|---------------------|
| n: 25 | 0.001         | 0.001    | 0.001               |
| n: 30 | 0.008         | 0.011    | 0.011               |
| n: 35 | 0.086         | 0.122    | 0.116               |
| n: 40 | 0.879         | 1.279    | 1.272               |
| n: 45 | 9.470         | 14.351   | 14.067              |
| n: 50 | 124.874       | 152.324  | 153.161             |

#### Firefox

| n     | JavaScript(s) | kirok(s) | kirok + Binaryen(s) |
|-------|---------------|----------|---------------------|
| n: 25 | 0.001         | 0.001    | 0.001               |
| n: 30 | 0.014         | 0.010    | 0.06                |
| n: 35 | 0.154         | 0.115    | 0.044               |
| n: 40 | 1.727         | 1.317    | 0.484               |
| n: 45 | 19.499        | 14.540   | 5.339               |
| n: 50 | 223.569       | 164.164  | 60.031              |

Chrome에서 kirok의 성능이 나쁜 이유는 알 수 없습니다. Firefox에서는 kirok이 더 빠르며, Binaryen을 사용하면 3배 이상 빨라집니다.

## Too Much Fibonacci
10부터 30까지 10씩 증가시키며 테스트합니다.
각 테스트를 500번에서 2500번까지 500씩 증가시키며 반복하고 총 시간을 측정합니다.
```html
<!-- App.svelte -->
<script lang="ts">
    import { onMount } from "svelte";
    import { useFibonacci } from "./kirok/Fibonacci";

    const [fibonacci, { calculate }] = useFibonacci();
    function jsFibonacci(n: number): number {
        if (n < 2) return n;
        return jsFibonacci(n - 1) + jsFibonacci(n - 2);
    }

    onMount(() => {
        for (let i = 5; i <= 30; i += 5) {
            for (let j = 500; j <= 2500; j += 500) {
                const jsStart = performance.now();
                for (let k = 0; k < j; k++) {
                    jsFibonacci(i);
                }
                const jsEnd = performance.now();
                const jsPerf = jsEnd - jsStart;

                $fibonacci.n = i;
                const start = performance.now();
                for (let k = 0; k < j; k++) {
                    calculate();
                }
                const end = performance.now();
                const wasmPerf = end - start;

                console.log(
                        `i=${i} j=${j} JS ${jsPerf.toFixed(
                                2
                        )}ms WASM ${wasmPerf.toFixed(2)}ms`
                );
            }
        }
    });
</script>
```

### 테스트 결과
낮은 숫자가 더 빠릅니다.
#### Chrome

| i, j         | JavaScript(ms) | kirok(ms) | kirok + Binaryen(ms) |
|--------------|----------------|-----------|----------------------|
| i=5  j=500   | 0.3            | 12.9      | 11.9                 |
| i=5  j=1000  | 0.1            | 13        | 14.1                 |
| i=5  j=1500  | 0.2            | 17.9      | 17.1                 |
| i=5  j=2000  | 0.2            | 21.5      | 21.3                 |
| i=5  j=2500  | 0.1            | 28        | 27.1                 |
| i=10  j=500  | 0.3            | 5.7       | 5.7                  |
| i=10  j=1000 | 0.4            | 11.6      | 11.3                 |
| i=10  j=1500 | 0.6            | 17.2      | 17                   |
| i=10  j=2000 | 0.9            | 23.7      | 24.4                 |
| i=10  j=2500 | 1.2            | 29.1      | 27.9                 |
| i=15  j=500  | 2.5            | 10.6      | 10.5                 |
| i=15  j=1000 | 5              | 22.6      | 19.6                 |
| i=15  j=1500 | 7.7            | 29.2      | 28.5                 |
| i=15  j=2000 | 10             | 38.5      | 39                   |
| i=15  j=2500 | 12.5           | 48.4      | 46.8                 |
| i=20  j=500  | 27.7           | 48.6      | 47.8                 |
| i=20  j=1000 | 55.5           | 96.7      | 95.6                 |
| i=20  j=1500 | 83.6           | 145.9     | 142.6                |
| i=20  j=2000 | 111.7          | 193.2     | 191.1                |
| i=20  j=2500 | 139.4          | 241.6     | 251.6                |
| i=25  j=500  | 308.1          | 479       | 482.3                |
| i=25  j=1000 | 618.5          | 963.9     | 948.9                |
| i=25  j=1500 | 920.3          | 1451.4    | 1432.3               |
| i=25  j=2000 | 1231.5         | 1903.6    | 1924.2               |
| i=25  j=2500 | 1549.2         | 2379.1    | 2406.6               |
| i=30  j=500  | 3418           | 5272.3    | 5297.4               |
| i=30  j=1000 | 7054.6         | 10686.8   | 10568                |
| i=30  j=1500 | 10516.5        | 15740.2   | 15740                |
| i=30  j=2000 | 13610.6        | 20398.6   | 20512.7              |
| i=30  j=2500 | 16733.8        | 25520.6   | 25529.6              |

#### Firefox

| i, j        | JavaScript(ms) | kirok(ms) | kirok + Binaryen(ms) |
|-------------|----------------|-----------|----------------------|
| i=5 j= 500  | 0              | 16        | 10                   |
| i=5 j=1000  | 1              | 15        | 10                   | 
| i=5 j=1500  | 0              | 13        | 9                    | 
| i=5 j=2000  | 0              | 14        | 10                   | 
| i=5 j=2500  | 0              | 18        | 13                   | 
| i=10 j=500  | 0              | 3         | 4                    | 
| i=10 j=1000 | 1              | 8         | 5                    |
| i=10 j=1500 | 2              | 15        | 9                    |
| i=10 j=2000 | 1              | 18        | 14                   |
| i=10 j=2500 | 2              | 18        | 15                   |
| i=15 j=500  | 5              | 6         | 3                    |
| i=15 j=1000 | 11             | 11        | 8                    |
| i=15 j=1500 | 16             | 17        | 12                   |
| i=15 j=2000 | 21             | 23        | 16                   |
| i=15 j=2500 | 26             | 27        | 22                   |
| i=20 j=500  | 61             | 22        | 16                   |
| i=20 j=1000 | 119            | 42        | 35                   |
| i=20 j=1500 | 178            | 64        | 53                   |
| i=20 j=2000 | 237            | 86        | 68                   |
| i=20 j=2500 | 297            | 104       | 85                   |
| i=25 j=500  | 655            | 200       | 163                  |
| i=25 j=1000 | 1315           | 402       | 330                  |
| i=25 j=1500 | 1977           | 601       | 486                  |
| i=25 j=2000 | 2650           | 803       | 650                  |
| i=25 j=2500 | 3315           | 1020      | 812                  |
| i=30 j=500  | 7358           | 2199      | 1764                 |
| i=30 j=1000 | 14490          | 4361      | 3533                 |
| i=30 j=1500 | 21756          | 6589      | 5300                 |
| i=30 j=2000 | 29389          | 8852      | 7044                 |
| i=30 j=2500 | 36919          | 10982     | 8860                 |

kirok은 많은 연산이 필요한 작업에서 더 빠르며,
많이 호출하는 것은 성능에 부정적인 영향을 미칩니다. 특히 적은 연산량에서는
이 영향이 더욱 눈에 띕니다. 이 영향 때문에 Binaryen의 성능 향상이 적어 보입니다.

## Count char count
랜덤한 문자열을 생성하고, 문자열에 포함된 각 문자의 개수를 셉니다.
```kotlin
@Serializable
@Model
// Map 타입이 직렬화되지 않으므로 List<Pair<String, Int>>로 대체합니다.
data class CharCount(var text: String, var count: List<Pair<String, Int>>)

@Init
fun initCharCount(): CharCount = CharCount("", listOf())

@Intent
fun count(charCount: CharCount) {
    val text = charCount.text
    val count = mutableMapOf<String, Int>()
    for (c in text) {
        count[c.toString()] = (count[c.toString()] ?: 0) + 1
    }
    charCount.count = count.toList()
}
```

```html
<!-- App.svelte -->
<script lang="ts">
    import { onMount } from "svelte";
    import { useCharCount } from "./kirok/CharCount";

    const [charCount, { count }] = useCharCount();
    
    function jsCharCount(str) {
        const count = {};
        for (let i = 0; i < str.length; i++) {
            const c = str[i];
            count[c] = (count[c] || 0) + 1;
        }
        return count;
    }

    onMount(() => {
        // 1,000,000 ~ 10,000,000 자의 문자열(1MB~10MB) 을 백만 자씩 늘려가며 생성합니다.
        
        for (let i = 1; i <= 10; i++) {
            const text = Array(i * 1_000_000)
                    .fill(0)
                    .map(() => String.fromCharCode(Math.floor(Math.random() * 26) + 97))
                    .join("");
            console.log(text);
            
            let start = performance.now();
            let result = jsCharCount(text);
            let end = performance.now();
            console.log(`js: ${end - start}ms`);
            console.log(result);
            
            start = performance.now();
            $charCount.text = text;
            count();
            end = performance.now();
            console.log(`kirok: ${end - start}ms`);
        }
    });
</script>
```

### 테스트 결과
낮은 숫자가 더 빠릅니다.
#### Chrome

| i          | JavaScript(ms) | kirok(ms) | kirok + Binaryen(ms) |
|------------|----------------|-----------|----------------------|
| 1,000,000  | 12             | 1148      | 926                  |
| 2,000,000  | 24             | 2243      | 1807                 | 
| 3,000,000  | 36             | 3385      | 2708                 | 
| 4,000,000  | 46             | 4268      | 3662                 | 
| 5,000,000  | 57             | 5387      | 4782                 | 
| 6,000,000  | 89             | 6357      | 5541                 | 
| 7,000,000  | 79             | 7618      | 6547                 |
| 8,000,000  | 93             | 8768      | 7566                 |
| 9,000,000  | 102            | 9752      | 8382                 |
| 10,000,000 | 116            | 10934     | 9461                 |

#### Firefox

| i          | JavaScript(ms) | kirok(ms) | kirok + Binaryen(ms) |
|------------|----------------|-----------|----------------------|
| 1,000,000  | 12             | 524       | 489                  |
| 2,000,000  | 18             | 1026      | 989                  | 
| 3,000,000  | 27             | 1562      | 1468                 | 
| 4,000,000  | 36             | 2063      | 1959                 | 
| 5,000,000  | 46             | 2597      | 2454                 | 
| 6,000,000  | 55             | 3149      | 2953                 | 
| 7,000,000  | 66             | 3675      | 3506                 |
| 8,000,000  | 74             | 4202      | 4043                 |
| 9,000,000  | 83             | 4788      | 4404                 |
| 10,000,000 | 92             | 5326      | 4870                 |

kirok은 대량의 데이터를 사용해 호출할 때 50배 느렸습니다. 대량의 데이터보다는 대량의 연산이 필요한 작업에 적합합니다.