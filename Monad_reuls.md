# 스칼라 초중급자를 위한 모나드 해설(2) - 모나드 규칙

앞에서는 모나드들과 모나드 규칙에 대해 살펴보았다.

먼저 모나드 규칙을 다시 적어보자.

1. `(unit x) bind f == f(x)` : (`return`은 `bind`에 대해 왼쪽 항등원임)

2. `m bind return == m` : (`return`은 `bind`에 대해 오른쪽 항등원임)

3. `(m bind f) bind g == m bind  { \x -> ((f x) bind g ) }`

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

따라서, _`unit`이 만들어내는 부가정보는 기존 모나드값의 부가정보 `뒤`에 덧붙여도 아무 변화가 없는 부가정보여야 한다._

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









