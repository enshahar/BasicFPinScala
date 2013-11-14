# 스칼라 초중급자를 위한 모나드 해설(2) - 모나드 규칙

앞에서는 모나드들과 모나드 규칙에 대해 살펴보았다.

먼저 모나드 규칙을 다시 적어보자.

1. `(unit x) bind f == f(x)`

2. `m bind return == m`

3. ` (m bind f) bind g == m bind  { \x -> ((f x) bind g ) }`

스칼라에서 2항 연산을 사용하기 위해 부득이 클래스 내부로 bind 함수(mkCFun함수)를 넣어야 한다.
이때 타입을 인자로 받게 해서 제네릭 클래로 만든다.
앞에서 다뤘던 클래스 중에서 `Logged`와 `List`만 변환하고, 나머지는 독자에게 숙제로 남겨둔다.

로깅의 경우 다음과 같이 쓸 수 있다.
```
case class Logged[T](value:T, log:List[String]) 
{ 
  def bind(f:T=>Logged[Int]) =  {
    val value2 = f(value)  // 함수 적용. value는 본 클래스의 필드임에 유의하라.
    value2 // 값 반환 
  }
}

object Logged {
  def Unit[T](x:T):Logged[T] = Logged(x, List())
}

```



