# 스칼라 초중급자를 위한 모나드 해설(2) - 모나드 규칙

앞에서는 모나드들과 모나드 규칙에 대해 살펴보았다.

모나드에 관련된 함수들을 살펴보자.

1. 어떤 클래스 `C`를 만들어낼 수 있는 생성자. 당연히 클래스 구현에 따라, 필요한 정보에 따라 구현은 천차만별이다.

2. 어떤 기저타입 `T`를 가지고 최소한의 정보를 가진 `C[T]` 타입을 만들 수 있는 `unit`함수. 이때 '정보'는 클래스 종류에 따라 달라진다.

3. `C[A]->(A->C[B])->C[B]` 타입의 `bind`함수. 이 함수는 `모나드 규칙`을 만족하도록 주의깊게 작성되어야 한다.


먼저 모나드 규칙을 다시 적어보자.

1. `(unit x) bind f == f(x)` : (왼쪽 항등원)

2. `m bind return == m` : (오른쪽 항등원)

3. `(m bind f) bind g == m bind  { \x -> ((f x) bind g ) }` : (결합법칙)

한가지 유의할것은 `bind`로 연결되는 함수들 또한 여러분이 하고 싶은 작업에 따라 직접 작성되어야 한다는 점이다.
기본적으로는 `A->C[V]`라는 함수를 만들때는 기저값 사이 변환 `A->V`에 대해 무언가 추가 정보를 `C`에 더 담는다고
생각하면 된다. 

스칼라에서 2항 연산을 사용하기 위해 부득이 클래스 내부로 bind 함수(mkCFun함수)를 넣어야 한다.
이때 타입을 인자로 받게 해서 제네릭 클래로 만든다.
앞에서 다뤘던 클래스 중에서 `Logged`와 `List`만 변환하고, 나머지는 독자에게 숙제로 남겨둔다.

로깅의 경우 다음과 같이 쓸 수 있다.
```
case class Logged[T](value:T, log:List[String]) 
{ 
  def bind(f:T=>Logged[Int]) =  {
    val value2 = f(value)      // 함수 적용. value는 본 클래스의 필드임에 유의하라.
    val log2 = log:::value2.log // 로그 병합. log는 본 클래스의 필드이다.
    Logged(value2.value, log2)
  }
}

object Logged {
  def unit[T](x:T):Logged[T] = Logged(x, List())
}

def doubleLogged(x:Int):Logged[Int] = Logged(x+x, List("double("+x+") = " + (x+x)))
def sqrtLogged(x:Int):Logged[Int] = Logged(Math.sqrt(x).toInt, List("sqrt("+x+").toInt = " + Math.sqrt(x).toInt))
```

이 예제를 가지고 앞에서 다뤘던 함수 합성을 2항연산으로 표현해 보자.

```
scala> val x1 =  doubleLogged(10) bind sqrtLogged
x1: Logged[Int] = Logged(4,List(double(10) = 20, sqrt(20).toInt = 4))
```

리스트의 경우 다음과 같다. 

```
abstract class MyList[+A] {
  // cons의 경우 결합법칙이 오른쪽이어야 한다. 
  // 스칼라에서는 이를 위해서 메소드 이름이 :로 시작해야 한다.
  // 이를 만족시키기 위해 표준 리스트 연산 ::를 정의했다.
  def ::[B >: A] (x: B): MyList[B] = Cons(x, this)

  // append도 마찬가지로 오른쪽 결합이다.
  // 이름도 표준라이브러리와 같게 만들자.
  def :::[B >: A](l: MyList[B]): MyList[B] = l match {
      case MyNil => this
      case Cons(h,t) => h::t:::this
  }

  // mapAll도 밖으로 빼자. 유용할것 같아 보이지 않나?
  // 그리고, 함수 타입을 좀 더 일반적인 타입으로 잡자.
  def mapAll[V](f:A=>MyList[V]):MyList[V] = this match {
    case Cons(h,t) => {
      val value2 = f(h)
      val remain = t.mapAll(f)
      value2:::remain
    }
    case MyNil => MyNil
  }
  
  def bind[V](f:A=>MyList[V]) = {
    // f가 만드는 리스트를 모두 합쳐야 하기 때문에 결과적으로는 
    // flatMap과 비슷한 일을 해야 한다.
    mapAll(f)
  }
}

object MyList {
  def unit[T](x:T):MyList[T] = Cons(x,MyNil)
}

case class Cons[B](var hd: B, var tl: MyList[B]) extends MyList[B]

case object MyNil extends MyList[Nothing]

def doubleMyList(x:Int):MyList[Int] = Cons(x+x,MyNil)
def sqrtMyList(x:Int):MyList[Int] = Cons(Math.sqrt(x).toInt,MyNil)
```

이 예제를 가지고 앞에서 다뤘던 함수 합성을 2항연산으로 표현해 보자.

```
scala> val x2 =  doubleMyList(10) bind sqrtMyList
x2: MyList[Int] = Cons(4,MyNil)
```

이제 실제 모나드 규칙을 살펴볼 수 있게 되었다.

그 전에 잠깐 하스켈식 모나드 표현을 살펴보자.

하스켈에서는 `return`(=`unit`)`, `>>=`(=`bind`)으로 모나드를 정의하며,
편리문법으로 `do {}`를 제공한다. 다음은 `do` 규칙이다.

```
  do e1 ; e2      =        e1 >> e2
  do p <- e1; e2  =        e1 >>= \p -> e2
```

여기서 `e1 >> e2`는 `e1 >>= {(_) >>= e2}`로 바인딩한 변수를 무시할뿐이다. 따라서 이 식은 따로 검토할 필요가 없다.

## `m bind unit` == `m` 의 의미

잘 살펴보자. 이미 정보가 추가된 모나드 값 `m`에 대해 `unit`을 `bind` 했다.

한번 앞의 리스트와 `Logged`에 대해 이를 살펴보자.

```
val m1 = Logged(1,List("log1","log2"))
val m2 = Cons(1, Cons(2, Cons(3, MyNil)))
m1 bind (Logged.unit)
m2 bind (MyList.unit)
```

결과는 다음과 같이 원래의 모나드 값과 같다.

```
scala> val m1 = Logged(1,List("log1","log2"))
m1: Logged[Int] = Logged(1,List(log1, log2))

scala> val m2 = Cons(1, Cons(2, Cons(3, MyNil)))
m2: Cons[Int] = Cons(1,Cons(2,Cons(3,MyNil)))

scala> m1 bind (Logged.unit)
res30: Logged[Int] = Logged(1,List(log1, log2))

scala> m2 bind (MyList.unit)
res31: MyList[Int] = Cons(1,Cons(2,Cons(3,MyNil)))
```

모나드 값에는 기저타입의 정보와 추가되는 다른 정보가 저장된 것으로 볼 수 있다고 했다.
이제 이 두가지 정보를 구현한다는 관점에서 `unit`과 `bind`를 생각해 보자.

위 제목의 조건은 어떤 모나드 `m`을 만든 다음 `unit`을 `bind`해도 `m`에 있던 정보(기저타입인 `T` 타입의 값 뿐 아니라, 
`C` 클래스에 저장된 추가 정보도 포함)가 그대로 유지됨을 보여준다.

부가정보는 f가 손을 댈 수가 없다(f가 받을 수 있는 값은 기저 타입의 값으로, f는 기저 타입의 값을 변환한 값에 
추가 정보를 더 부여해모나드를 반환할 뿐이다). 따라서 부가정보를 유지하고 넣어줄 함수는 `bind`밖에 없다. 

따라서, _모나드 값 `m`에서 부가정보를 꺼내고, `unit`를 적용한 결과 나오는 부가정보를 추가해서, 다시 모나드 타입의 값으로 감싸주는 기능을 `bind`에 구현해야 한다._

모나드 내에 저장될 기저 타입의 값에 대해서는 `bind`가 `m`에 저장된 값을 유지하는 한, 이 규칙을 위배하지 않는다. 


이제, `unit` 측면에서 보자. `unit`은 모나드 값을 만들어낸다. 이 모나드 값은 어떤 특성을 가져야 할까?

앞에서 `bind`가 `m`의 부가정보를 `unit`이 반환한 모나드 객체에 들어있는 값에 있는 부가정보에 덧붙인다고 했다.
그런데 이렇게 만들어진 부가정보는 `m`의 부가정보와 같아야 한다.

따라서, _`unit`이 만들어내는 부가정보는 기존 모나드값의 부가정보 `뒤`에 덧붙여도 아무 문제가 없는 부가정보여야 한다._

또한, _`unit`은 기존 모나드 기저 타입의 값을 새 모나드에 그대로 유지해야 한다._ 

그렇지 않다면(새 모나드에 다른 기저타입 값을 반환한다면), `bind`가 `unit`이 반환한 기저타입 값을 새 모나드에 넣는 경우
(물론 모나드의 `bind`에 따라서는 이렇게 하지 않은 경우도 있을지 모른다.) `m`에 저장된 기저타입 값과 같은 값이 나오지 않게 된다.
 
### 하스켈 문법 측면에서 보기 

다음으로 참고로 하스켈 문법 측면에서 보자. 관심없는 사람은 다음 절로 넘어가도 된다.

좌항 `m bind unit`에서 `unit`을 에타확장 시키면 
`m bind ((x)=>unit(x)))`이고, 이를 하스켈 식으로 쓰면, `m >>= \x -> unit(x)` 이다.
따라서 이를 do 표현식으로 쓰면 `do { x <- m; return x }`가 된다. 
절차중심의 프로그래밍과 같이 생각한다면 `x<-m`은 `x`에 `m`을 대입하는 것이고, `return x`는 `x`값을 반환하는 것 처럼 보인다.
그런데 x는 m이므로 이는 m을 반환하는 것과 같으리라 유추하기 쉽다.

우항 m은 do로 써도 `do { m }`이다. 따라서 이식도 m을 반환하는 거라 유추할 수 있다. 
_그런데 이는 정확히 앞에서 하스켈 `do` 식을 절차중심 프로그래밍처럼 생각해 유추한 것과 동일한 결과이다._

## `(unit x) bind f` == `f(x)`

먼저 한번 리스트와 `Logged`에 대해 테스트 해보자.

```
Logged.unit(2) bind doubleLogged
MyList.unit(3) bind doubleMyList
doubleLogged(2)
doubleMyList(3)
```

결과를 보자.

```
scala> Logged.unit(2) bind doubleLogged
res38: Logged[Int] = Logged(4,List(double(2) = 4))

scala> MyList.unit(3) bind doubleMyList
res39: MyList[Int] = Cons(6,MyNil)

scala> doubleLogged(2)
res40: Logged[Int] = Logged(4,List(double(2) = 4))

scala> doubleMyList(3)
res41: MyList[Int] = Cons(6,MyNil)
```

`res38`과 `res40`, `res39`와 `res41`이 일치함을 볼 수 있다.

앞의 절에서 생각했던 조건을 여기서도 비슷하게 생각해낼 수 있다.

_`unit`이 만들어내는 부가정보는 기존 모나드값의 부가정보 `앞`에 넣어도 아무 변화가 없는 부가정보여야 한다._
_`bind`는 모나드에 들어있는 기저타입 값을 `f`에 그대로 전달해야 한다. 아니면 적어도 `f(x)`했을때와 같은 효과가 나타나는 기저타입 값을 전달해야 한다._
_`unit`이 만들어내는 모나드에 들어있는 기저타입 값은 원래 `unit`에 넘긴 값을 그대로 유지해야 한다._

한가지 더 보자면, `unit`도 `T->C[T]`타입의 함수이므로 `(unit x) bind f == f(x)`의 `f`에 `unit`을 넘기면, 
`(unit x) bind unit == unit(x)`이 된다. 따라서 실제로는 `unit`은 아무리 많이 바인드를 해도 정보 추가나 
기저타입 값의 변화를 일으키지 않는 함수여야 한다.

리스트 등의 경우를 한번 보자.
```
(MyList.unit(1)) bind MyList.unit
(MyList.unit(2)) bind MyList.unit bind MyList.unit
Logged.unit(3) bind Logged.unit
Logged.unit(4) bind Logged.unit bind Logged.unit
```

결과는 다음과 같다.
```
scala> (MyList.unit(2)) bind MyList.unit
res46: MyList[Int] = Cons(2,MyNil)

scala> (MyList.unit(2)) bind MyList.unit bind MyList.unit
res47: MyList[Int] = Cons(2,MyNil)

scala> Logged.unit(3) bind Logged.unit
res48: Logged[Int] = Logged(3,List())

scala> Logged.unit(3) bind Logged.unit bind Logged.unit
res49: Logged[Int] = Logged(3,List())
```


### 하스켈 문법 측면에서 보기 

좌항 `(unit x) bind f`를 하스켈 연산자로 다시쓰면, `(return x) >>= f`이다. 다시 에타 확장 시키면, 
`(return x) >>= (\y -> f(y))`가 된다. 이를 `do`로 바꿔쓰면 `do { y <- return x; f(y) } `이다. 
우항은 그대로 `do { f(x) }`이다. 이 또한 절차지향적인 생각으로 해석을 한다면 `y`에 `x`를 대입한 다음 
`f(y)`를 한 것으로 볼 수 있다.

앞에서 분석한 문법과 같이 본다면, 

`do { x <- m; return x }` = `do { m }`
`do { y <- return x; f(y) }` = `do { f(x) }`

이다. 여기서 `do`내에서는 `<-`는 일종의 대입(또는 변수에 값을 바인딩)으로 생각하고,
`return`은 항등함수과 마찬가지로 생각할 수 있음을 보여준다.


### `(m bind f) bind g` == `m bind  { \x -> ((f x) bind g ) }`

먼저 테스트를 해보자.

```
(Logged(8,List("log1","log2")) bind doubleLogged) bind sqrtLogged
Logged(8,List("log1","log2")) bind { (x) => (doubleLogged(x) bind sqrtLogged) }
((2::8::MyNil) bind doubleMyList) bind sqrtMyList
(2::8::MyNil) bind { (x) => (doubleMyList(x) bind sqrtMyList) }
```

결과는 다음과 같다.
```
scala> (Logged(8,List("log1","log2")) bind doubleLogged) bind sqrtLogged
res51: Logged[Int] = Logged(4,List(log1, log2, double(8) = 16, sqrt(16).toInt = 4))

scala> Logged(8,List("log1","log2")) bind { (x) => (doubleLogged(x) bind sqrtLogged) }
res52: Logged[Int] = Logged(4,List(log1, log2, double(8) = 16, sqrt(16).toInt = 4))

scala> ((2::8::MyNil) bind doubleMyList) bind sqrtMyList
res53: MyList[Int] = Cons(2,Cons(4,MyNil))

scala> (2::8::MyNil) bind { (x) => (doubleMyList(x) bind sqrtMyList) }
res54: MyList[Int] = Cons(2,Cons(4,MyNil))
```

앞의 두 절에서 본 예들과 지금의 예를 볼 때 `MyList`와 `Logged`는 모나드 타입 성질을 만족함을 알 수 있다.

제목에 있는  결합 법칙을 살펴보자.

왼쪽 항 `(m bind f) bind g`은 어떤 모나드 값에 `f`를 바인드해 만든 새로운 모나드 값에 `g`를 바인드하는 것이다.

오른쪽 항을 실제 호출한다면 먼저 `f(x)`가 호출되어 거기에 g가 바인드된다. 

실제로 이 결합법칙을 기저타입의 값과 추가정보로 분리해 생각해 보면 다음과 같다.

오른쪽 항을 생각해 볼 떄, 기저타입의 값은 `m`에 있는 값이 추출되어 `{ \x -> ((f x) bind g ) }`에 전달될 것이다.
그 후 `f(x)`가 호출 되고, 그 다음에 `g`가 바인드되므로 _`g`에 `m`에 있던 기저타입 값에 `f`가 적용된 값이 전달_ 되어 
최종 모나드 값에 저장된다. 이 관계는 왼쪽항의 경우에도 마찬가지이다. 따라서 `f`, `g`에 기저타입 값이 전달되는 순서는 
변경되지 않기 때문에, 최종 기저타입 값도 변화될 여지가 없다.

하지만, 추가정보의 경우에는 어떨까?

왼쪽항은 `m`에 있던 추가정보와 `f` 적용 결과 생긴 모나드의 정보가 조합된 다음에 `g` 적용 결과 생긴 모나드의 정보가 조합된다.
반면 오른쪽 항에서는 먼저 무명 함수 몸체 안의 `bind`에 의해 `f` 적용 결과 생긴 모나드의 정보에 `g` 적용 결과 생긴 모나드의 정보가 
조합되고, 그 다음에서야 `m`에 있던 정보가 `bind`에 의해 앞부분이 추가된다.

따라서, _`bind`에서 추가정보를 결합하는 로직은 결합법칙이 성립해야 한다._

### 하스켈 문법 구조에서 설명

`(m bind f) bind g`과 `m bind  { \x -> ((f x) bind g ) }`을 보자.

먼저 좌변은 `(m >>= f) >>= g`라고 쓸 수 있고, 여기서 `g`를 에타확장하면 `(m >>= f) >>= (\y => g(y))`가 된다.
이 패턴은 `do` 패턴에 맞는다. 따라서 이를 적용하면 do { y <- (m>>=f); g y }가 되고, `m>>=f`를 에타확장해서 
`do`패턴에 맞추면 `do { x <- m; f x }`이다. 결국 전체 식은 `do { y <- do { x <- m; f x}; g y}`가 된다.

그런데 좌변은 다시 `m >>= (\x->f(x)) >>= (\y->g(y))`라고 표현할 수 있다(에타확장).
이를 하스켈 `do`규칙에 따라 앞에서부터 차례로 적용하면, `do { x <- m; y <- f x; g y }`이 된다. 

우변을 보자. 우변은 `m >>= {\x -> ((f x) bind g)}`이다. 먼저 맨 바깥쪽 바인딩은 `do` 규칙에 완벽히 들어맞는다.
규칙을 한번 적용하면 `do { x <- m; (f x) bind g }`가 된다. 

다시 안쪽 `(f x) bind g`를 보면 `(f x)>>=g`이고, 이를 에타확장 시켜서 `(f x)>>=(\y->g(y))`로 만들면 
do 규칙에 맞는다. 이는 `do { y <- f(x); g y}`가된다. 

따라서 우변의 최종 식은 `do { x <- x; do { y<-f x; g y } }` 가 된다. 

즉, 

`do { x <- m; y <- f x; g y }`
`do { y <- do { x <- m; f x}; g y}`
`do { x <- m; do { y<-f x; g y } }`

이 세 식이 같다는 말이다. 


## 모나드 법칙이 중요한 이유

개인적으로는 구현 측면에서 볼 때 `모나드 법칙`이 중요한 이유는 
만들어진 클래스가 모나드 성질을 만족해서 함수언어들이 제공하는 구조(하스켈의 `do`나 함수언어의 `for`)에 
자연스럽게 녹아들어가는지 검사할 때 필요하기 때문인 것 같다. (즉, 일반 사용자의 경우에는 
`do`나 `for`를 사용하는 법만 알아도 될것 같다.) 하지만 무언가 `for`등에 쓰일 구조를 만들어야 한다면 이때는 
검증을 위해서라도 모나드 법칙등을 검사할 필요가 있을 것이다.

하지만, 앞에서 나왔던 여러 조건들을 정리하면 다음과 같은 4가지가 된다.

1. _`unit`이 만들어내는 부가정보는 부가정보를 결합하는 연산에 대해 어떤 정보도 추가/삭제하지 않는다._

2. _`unit`은 기존 모나드 기저 타입의 값을 새 모나드에 그대로 유지해야 한다._ 

3. _`bind`에서 추가정보를 결합하는 로직은 결합법칙이 성립해야 한다._

4. _`bind`는 모나드에 들어있는 기저타입 값을 `f`에 그대로 전달해야 한다. 아니면 적어도 `f(x)`했을때와 같은 효과가 나타나는 기저타입 값을 전달해야 한다._


`MyList`의 경우를 보자.

기저타입은 정수이고, 부가정보로 추가되는 것은 리스트화되면서 들어간 정보이다. 아마도 원소의 순서를 
정보로 볼 수 있을 것이다. 리스트라는 것이 원소간의 순서를 유지하기 위한 것이라 생각할 수 있으니까. 

`unit`의 경우 기저타입을 감싼 1항목짜리 리스트에 어떤 원소를 추가하거나 삭제하거나 하지 않는다. 
따라서 이 경우 `unit`은 추가되는 정보는 없고, 타입 변환만을 수행한다고 볼 수 있다.

2는 구현부를 보면 그대로 기저값을 리스트에 넣기만 하므로 성립한다.

3은 (List1 ::: List2) ::: List3 = List1:::(List2:::List3) 이므로 성립한다.

4는 바인드 구현을 보면 된다. 리스트에서 각 원소를 `순서대로` 방문하면서 f를 적용해 
나온 결과값을 `:::`로 순서를 맞춰 결합시키고 있다. 따라서 이또한 성립된다.

`Logged`의 경우는 어떨까?
기저타입은 정수였고, 부가정보는 로그 리스트이다.

1. `unit`의 경우 로그로 빈 리스트를 남긴다. 빈 리스트는 `:::`에 대해 항등원이다.

2. `unit`에서 기반타입 값은 그대로 `Logged`의 `value`필드에 저장된다.

3. (List1 ::: List2) ::: List3 = List1:::(List2:::List3) 이므로 성립한다. 

4. 역시 bind 내에서 보면 `value`필드를 `f`에 적용시키고 있다. 

이렇게 앞의 4가지 법칙을 사용하면 좀 더 쉽게 모나드 구성이 가능할 것 같다.
(엄밀하게 하려면 수학적으로 증명을 해야 하겠지만, 내 능력 밖의 일이다. T.T)

### 바인딩을 잘못한 경우의 한 예

모나드 법칙을 만족하지 않는 바인드 함수를 만든다면 어떨까? 타입은 만족시키지만 모나드 법칙을 만족시키지 않는 
함수를 만들 수 있을 것이다.

앞에서 로깅관련 바인딩 함수를 잘못 만들었던 것이 기억난다. 한번 이를 다시 적용해 보자.


```
case class LoggedJustOne[T](value:T, log:List[String]) 
{ 
  def bind(f:T=>LoggedJustOne[Int]) =  {
    val value2 = f(value)      // 함수 적용. value는 본 클래스의 필드임에 유의하라.
    LoggedJustOne(value2.value, value2.log)
  }
}

object LoggedJustOne {
  def unit[T](x:T):LoggedJustOne[T] = LoggedJustOne(x, List())
}

def doubleLoggedJustOne(x:Int):LoggedJustOne[Int] = LoggedJustOne(x+x, List("double("+x+") = " + (x+x)))
def sqrtLoggedJustOne(x:Int):LoggedJustOne[Int] = LoggedJustOne(Math.sqrt(x).toInt, List("sqrt("+x+").toInt = " + Math.sqrt(x).toInt))
```

컴파일 등은 문제가 없다. 한번 모나드 법칙을 검증해 보자.

```
//1. `(unit x) bind f == f(x)` : (왼쪽 항등원)
(LoggedJustOne.unit(10)) bind doubleLoggedJustOne
doubleLoggedJustOne(10)

//2. `m bind return == m` : (오른쪽 항등원)
LoggedJustOne(10, List("1","2")) bind LoggedJustOne.unit

//3. `(m bind f) bind g == m bind  { \x -> ((f x) bind g ) }` : (결합법칙)
(LoggedJustOne(10, List("1","2")) bind doubleLoggedJustOne) bind sqrtLoggedJustOne
LoggedJustOne(10, List("1","2")) bind { (x)=> (doubleLoggedJustOne(x) bind sqrtLoggedJustOne) }

```

결과는 다음과 같다.
```
scala> (LoggedJustOne.unit(10)) bind doubleLoggedJustOne
res62: LoggedJustOne[Int] = LoggedJustOne(20,List(double(10) = 20))

scala> doubleLoggedJustOne(10)
res63: LoggedJustOne[Int] = LoggedJustOne(20,List(double(10) = 20))

scala> LoggedJustOne(10, List("1","2")) bind LoggedJustOne.unit
res64: LoggedJustOne[Int] = LoggedJustOne(10,List())

scala> (LoggedJustOne(10, List("1","2")) bind doubleLoggedJustOne) bind sqrtLoggedJustOne
res65: LoggedJustOne[Int] = LoggedJustOne(4,List(sqrt(20).toInt = 4))

scala> LoggedJustOne(10, List("1","2")) bind { (x)=> (doubleLoggedJustOne(x) bind sqrtLoggedJustOne) }
res66: LoggedJustOne[Int] = LoggedJustOne(4,List(sqrt(20).toInt = 4))
```

다른건 문제 없어 보이지만, 오른쪽 항등원 규칙에서 문제가 발생한다.

물론 바인등을 어떻게 잘못 만들었느냐에 따라 차이가 날 수 있다.



