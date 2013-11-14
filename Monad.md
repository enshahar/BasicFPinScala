# 스칼라 초중급자를 위한 모나드 해설(1)

함수언어, 특히 하스켈과 같은 순수함수언어 계열을 다룰때는 반드시 마주치게 되며,
스칼라와 같은 하이브리드 언어를 사용하더라도 한번쯤은 관심을 가지게 되는 괴물(?)이 있으니 
바로 _모나드(Monad)_ 이다.

모나드에 대해 설명하는 튜토리얼을 들여다 보더라도 이해하기 쉽지가 않다. 
물론 대부분 영어로 되어있기 때문이라는게 첫번째 이유이고, 
두번쨰 이유는 추상적인 모나드 구조와 실제 우리가 코딩하는 코딩의 요소들 사이의 관계에 대해 
자세히 풀어 설명해주는 글이 드물다는 것이 두번쨰 이유이다. 

물론 스칼라에서 `map`과 `flatMap`이 정의된 클래스들을 `for`와 `yield`라는 
편리문법(syntactic sugar)과 결합해서 편리하게 다루는 것을 이미 본 사람이라면 
모나드가 뭔가 좋은일(?)을 하는 것이라는 정도는 알고 있지만, 
왜 대체 모나딕 룰이 필요한 건지, 대체 `flapMap`과 `map`이 둘 다 필요한 이유는 무엇인지 등등이 
궁금할 것이다.

나 또한 모나드를 100% 이해했다고는 할 수 없지만, 그간 들여다본 것을 바탕을
나름대로 모나드에 대해 설명해 보고자 한다. 카테고리 이론등은 알지도 못하고,
공부를 깊이 하지도 못했기 때문에 가능한 서술적으로 설명을 할 것이다. 
독자 여러분도 이 글을 100% 신뢰하지 말고 비판적으로 읽기 바란다. 

## 값을 감싼 타입 만들기: 기본 생성 함수

기본적으로는 어떤 값을 감싼 다른 타입을 만드는 것에서 이 모든 일이 비롯된 것이다.
어떤 값을 감싸는 이유는 외부에 대해 그 값을 감추거나(정보은닉), 
감싸면서 어떤 정보를 추가하거나, 추가적인 연산을 정의하기 위한 것이다.

스칼라에서 어떤 값을 감싼 타입을 만드는 가장 쉬운 방법은 케이스 클래스를 활용하는 것이다.

아래 코드는 단순히 `T` 타입의 값을 감싼 타입인 `Boxed[T]`로 바꿔주는 케이스 클래스를 정의한다.

```
case class Boxed[T](value:T);
```
조금 더 유용한 클래스를 만들자면 로그를 남기는 `Logged[T]`를 들 수 있다. 
값의 변화를 추적하면서 리스트에 로그를 남길 때 사용한다.

```
case class Logged[T](value:T, log:List[String])
```

또 다른 클래스로는 칼라의 `Option` 클래스를 들 수 있다. `Option`이라는 
이름이 의미하듯, 이 클래스는 어떤 값이 존재하거나(이때는 
`Some(value)`라는 타입이 된다), 어떤 값이 존재하지 않는(이때는 `None`이라는 하위 타입이 된다)
스칼라에서는 이런 경우 추상 클래스와 이를 상속한 하위 클래스들로 구현하게 된다.
이름 혼동을 막기 위해 `MyOption`이라는 이름을 사용한다.

```
abstract class MyOption[+A]

case class MySome[+A](x: A) extends MyOption[A]

case object MyNone extends MyOption[Nothing] 
```

또 다른 예로는 나중에 수행할 계산을 보관하기 위한 `Lazy`라는 이름의 클래스를 들 수 있다.

```
class Lazy[T](value: ()=>T) {
   def getValue():T = value()
}
```

다른 형태로는 컬렉션이 있다. 기본적 컬렉션의 하나인 리스트를 예로 들어보자.

```
abstract class MyList[+A] 

case class Cons[B](var hd: B, var tl: MyList[B]) extends MyList[B]

case object MyNil extends MyList[Nothing]
```

`Cons`는 머리(`hd`, 어떤 타입의 값)와 꼬리(`tl`, 리스트)로 이루어진 리스트이고, 
MyNil은 리스트의 끝(또는 아무 원소도 없는 리스트)을 표기하는 특별한 리스트이다. 
앞의 `MyOption`이나 `Lazy`등과 달리 여기서는 `MyList`가 재귀적으로 정의된다.

만약 위 정의만으로 정수 리스트 `MyList(1,2,8)`을 정의하려면 
```
val x1:MyList[Int] = Cons(1,Cons(2,Cons(8,MyNil)))
```
과 같이 해야 한다. 

이제 이 모든 클래스 `C[T]`에 대해 `T=>C[T]`타입인 생성함수를 만들 수 있다.
케이스 클래스라면 이미 스칼라가 제공하는 생성자를 사용할 수도 있다. 그렇지 않다면 직접 객체를 new로 만들어야 한다.
`Logged`와 같이 클래스의 인자(=필드)가 여러개인 경우에는 T를 튜플 타입으로 생각해서 처리하거나 할 수도 있겠지만,
편의를 위해 일단은 별도의 함수를 가정하자.

예제에서는 `C[Int]`를 위주로 설명할 것이므로, `Int => C[Int]` 타입의 함수를 정의할 것이다.
(엄밀히 말하면 REPL에서 def로 만드는 함수나 특정 클래스 안에 정의된 메소드는 함수와는 약간 다르다.
 제대로 알려면 스칼라 메소드와 함수 타입에 대해 이해를 해야 하지만, 자세한 설명은 생략한다. 관심 있는 독자는 
 [스택오버플로](http://stackoverflow.com/questions/2529184/difference-between-method-and-function-in-scala)나 
 [이 블로그글](http://jim-mcbeath.blogspot.com.au/2009/05/scala-functions-vs-methods.html)을 참조하라.)

```
// Int => C[Int] 생성 함수들
// 케이스클래스인 경우 기본제공되는 클래스 짝 객체에 있는  생성자함수를 사용 가능하며,
// 아닌 경우 new를 사용하자.
def initBoxed(x:Int):Boxed[Int] = Boxed(x)
def initLogged(x:Int):Logged[Int] = Logged(x, List())
def initMyOption(x:Int):MyOption[Int] = MySome(x)
def initLazy(x: =>Int):Lazy[Int] = new Lazy(()=>x)
def initMyList(x:Int):MyList[Int] = Cons(x,MyNil)
```

이런 함수가 있어야만 기저 타입(`C[T]`라면 `T`를 기저타입이라고 부르자. 당연하 상위/하위타입은 아니고, 
부모나 자식타입도 아니고, 제네릭 타입의 타입인자이긴 하지만 타입인자라 부르긴 또 그래서 이런 이름을 
붙였다)에서 클래스 객체를 만들어낼 수 있으므로, 이런 생성 함수는 반드시 있어야 한다. 또한, `C[T]`를 
만드는 이유는 무언가 `T`타입의 값만으로는 저장할 수 없는 추가 정보(꼭 값 뿐 아니라 동작상태, 순서 등도 
포함한 포괄적인 이야기이다)를 남기고 싶어서일 것이다.  

_감싸기 규칙0: 어떤 타입 `T`에 추가 정보를 더한 것이 C[T] 클래스이다. 당연하 추가 정보에 따라 클래스 구현은 달라진다._

_감싸기 규칙1: 어떤 타입 `T`를 최소한의 추가 정보와 함께 감싼 `C[T]`타입의 객체를 만드는 `T=>C[T]` 타입의 기본 생성 함수 가 있어야 한다._

## 연산을 적용한 결과를 감싸기

프로그래밍은 입력에서 출력까지의 일련의 변환 과정이며, 결국 함수의 조합이라 할 수 있다.
일단은 타입이 바뀌는 경우는 제외하고, 편의를 위해 같은 타입 안에서 일어나는 변환(
당연히 이런 경우에는 타입은 안 바뀌고 값만 바뀐다)만을 살펴보자. 

이제 정수 값을 2배로 만드는 `double`함수와 어떤 정수의 제곱근(실수값이 나오는 경우 정수로 내림)을 반환하는 
`sqrt` 함수를 생각해 보자.

```
def double(x:Int):Int = x + x

def sqrt(x:Int):Int = Math.sqrt(x).toInt
```

어떤 정수에 대해 이 함수들을 적용하고, 그 값을 앞에서 정의한 클래스 `C`로 감싸고 싶다.
물론 감쌀때는 클래스의 의미에 맞게 적절한 방식으로 감싸야만 한다.

`Boxed`의 경우는 그냥 감쌀뿐 특별히 하는 일은 없다.

```
def doubleBoxed(x:Int):Boxed[Int] = Boxed(x+x)
def sqrtBoxed(x:Int):Boxed[Int] = Boxed(Math.sqrt(x).toInt)
```

`Logged`의 경우 각 함수에 맞게 로그를 덧붙이면 될 것이다.
```
def doubleLogged(x:Int):Logged[Int] = Logged(x+x, List("double("+x+") = " + (x+x)))
def sqrtLogged(x:Int):Logged[Int] = Logged(Math.sqrt(x).toInt, List("sqrt("+x+").toInt = " + Math.sqrt(x).toInt))
```


`MyOption`의 경우 각 함수의 성질에 따라 오류라면 `MyNone`, 아니라면 `MySome`을 반환한다.
```
def doubleMyOption(x:Int):MyOption[Int] = MySome(x+x)
def sqrtMyOption(x:Int):MyOption[Int] = if(x>=0) MySome(Math.sqrt(x).toInt) else MyNone
```

`Lazy`는 바로 계산을 하지 않고 함수를 만들어서 나중에 필요할때 계산을 수행하게 만든다(수행시점을 
확인하기 위해 간단하게 프린트문을 추가했다). 이때 정수 연산을 미뤄둔다. 
안그러면 식을 인자로 주는 경우 해당 식이 평가되어 버린다.

```
def doubleLazy(x: =>Int):Lazy[Int] = new Lazy(()=>{print("lazy double(" + x + ") run\n");x+x})
def sqrtLazy(x: =>Int):Lazy[Int] = new Lazy(()=>{print("lazy sqrt(" + x + ") run\n");Math.sqrt(x).toInt})
```

`MyList`는 계산 결과만을 유일한 원소로 포함하는 리스트로 만든다.
```
def doubleMyList(x:Int):MyList[Int] = Cons(x+x,MyNil)
def sqrtMyList(x:Int):MyList[Int] = Cons(Math.sqrt(x).toInt,MyNil)
```

위 예에서는 `Int`를 받아 `Int`를 반환하는 대신에, 처리 결과와 추가 정보를 한데 묶어서 `C[T]` 타입의 
값을 반환하는 `Int=>C[Int]`타입의 함수를 만들었다. 

일반적인 타입 `T`에 대해도 마찬가지로 그 타입의 값을 처리해서 적절한 정보를 부가해서 `C[T]` 타입의 
값을 만들어내는 함수 `T=>C[T]`를 만들 수 있다.

## 조합해 나가기 

앞에서 프로그래래밍은 변환이며 함수 합성의 연속이라고 했다. 두 함수 `f:T=>U`와 `g:U=>V`가 있다면,
이 두 함수를 합성한 것은 수학적으로는  `g.f`라고 쓰고 `g(f(x))`와 같다.

이를 프로그램에서 사용할 때도 마찬가지이다. .를 쓰긴 좀 그러니 비슷한 `o`를 써서 함성함수를 
고차함수(high order function)로 만들어보자. 아래는 언커리된(uncurried) 형태의 `o` 함수이다.

```
def o[T,V,U](f:T=>V, g:V=>U) = (x:T) => g(f(x))
```

앞에서 봤던 `double`과 `sqrt`를 생각해 보자.

```
val doubleThenSqrt = o(double, sqrt)
```
이제 스칼라에서 실행해 보면 다음과 같다. (8+8)의 제곱근인 4가 제대로 나온다.
```
scala> doubleThenSqrt(8)
res0: Int = 4
```

필요하다면 커리(curried)된 형태로도 만들 수 있다. 

```
def o2[T,V,U](f:T=>V) = (g:V=>U) => (x:T) => g(f(x))
```

다음과 같이 사용할 수 있다.

```
scala> val doubleThenSqrt2Result = o2(double)(sqrt)(8)
doubleThenSqrt2Result: Int = 4
```

`o(sqrt,double)`로 두 함수를 함성했던 것처럼 부가정보가 붙은 연산들도 합성하고 싶다. 
합성할 수 없다면 제약이 너무 심하니까 꼭 가능했으면 한다.

가장 쉬운 `Boxed`로 무작정 한번 시도해 보자.
```
scala> o(doubleBoxed,sqrtBoxed)
<console>:13: error: type mismatch;
 found   : Int => Boxed[Int]
 required: Boxed[Int] => Boxed[Int]
              o(doubleBoxed,sqrtBoxed)
                            ^
```

기대한대로(?) 타입오류가 난다. 타입오류니까 타입을 맞출 방법을 생각해 보자.
`Int=>C[Int]`타입의 함수 둘울 합성하려면 첫번쨰 함수를 `Int=>Int` 타입으로 만들거나 
두번째 함수를 `C[Int]=>C[Int]` 타입으로 만들 수 있으면 된다. 굳이 `C[T]`를 만든 이유는
부가정보를 넣거나 부가적인 처리를 하기 위한 것인데, 첫번쨰 함수를 `Int=>Int` 타입으로 
만드는 것은 정보 손실이 일어나므로 부적합하다. 따라서 두번째 방법을 생각해보자.

박스로 감싸는 경우는 간단하다.  `Int=>Boxed[Int]` 타입의 함수 `f`를 받고, 
`Boxed[Int]` 타입의 값 `x`를 받아서, `x`의 내부 값을 벗겨내서 `f`를 적용한 다음에 
다시 박스로 감싸면 된다.

```
def mkBoxedFun(f:Int=>Boxed[Int]) = (x:Boxed[Int]) => {
	val value = x.value    // x(박싱된 값)에서 내부의 값 노출시키기
	val value2 = f(value)  // 함수 적용
	value2				   // 값 반환 
}
```

이제 이를 사용해 보자.
```
scala> val x2 = o(mkBoxedFun(doubleBoxed), mkBoxedFun(sqrtBoxed))(initBoxed(8))
x2: Boxed[Int] = Boxed(4)
```

`Logged`에 대해 정의해보자. 일단은 위 `mkBoxedFun`을 복사후 붙여넣기 하고, 타입만 맞춰보자.
웬지 감이 좋다.

```
def mkLoggedFun(f:Int=>Logged[Int]) = (x:Logged[Int]) => {
	val value = x.value    // x(로그 포함된 값)에서 내부의 값 노출시키기
	val value2 = f(value)  // 함수 적용
	value2				   // 값 반환 
}
```

감이 좋았는데, 역시 잘 컴파일되고 타입도 맞는것 같다. 내친김에 적용해보자.

```
scala> val x3 = o(mkLoggedFun(doubleLogged), mkLoggedFun(sqrtLogged))(initLogged(8))
x3: Logged[Int] = Logged(4,List(sqrt(16).toInt = 4))
```

문제 없나? 아니다. List에 보면 앞쪽 로그는 다 사라졌다. 함수 적용후 나온 value2에는 다른 정보는 없고, f를 적용한 정보만 
있기 때문이다. 역시 mkXXXFun도 구체적인 XXX 클래스의 종류에 따라 주의깊게 설계해야 함을 알 수 있다. 혹시나했는데 역시나이다.

다시 로그를 합치도록 해보자.

```
def mkLoggedFunRevised(f:Int=>Logged[Int]) = (x:Logged[Int]) => {
	val value = x.value    // x(로그 포함된 값)에서 내부의 값 노출시키기
	val log = x.log        // x에서 로그 가져오기
	val value2 = f(value)  // 함수 적용
	Logged(value2.value, log:::value2.log)
}
```

이제 실험해보자.
```
scala> val x4 = o(mkLoggedFunRevised(doubleLogged), mkLoggedFunRevised(sqrtLogged))(initLogged(8))
x4: Logged[Int] = Logged(4,List(double(8) = 16, sqrt(16).toInt = 4))
```

차례로 `double(8) = 16`, `sqrt(16).toInt = 4`이 들어간 리스트가 있다. `4`를 구하기까지 거쳐간 고난의 역사가 
거기 담겨있다. 좋다!


여기까지만 봐도 다음과 같은 말을 할 수 있을 것이다.

_감싸기 규칙2: 정보가 추가된 `T=>C[T]`타입의 끼리 합성하기 위해서는 `T=>C[T]` 타입을 `C[T]=>C[T]`로 바꿔주는 고차함수가 필요한데, 이 함수는 `C`의 종류에 맞게 `주의깊게` 설계해야 한다._

이제 `주의깊게` 다른 클래스에 대해서도 설계해 보자. 먼저 `MyOption`이다.
```
def mkMyOptionFun(f: (Int=>MyOption[Int])) = (x: MyOption[Int]) => x match {
  case MySome(x) => { // 값을 노출시키는 작업은 패턴매칭으로 됨
    val value2 = f(x) // f 적용하기
    value2 // 값 반납하기
  }
  case MyNone => MyNone	
}

val x5 = o(mkMyOptionFun(doubleMyOption), mkMyOptionFun(sqrtMyOption))(MySome(8))
val errval = o(mkMyOptionFun(doubleMyOption), mkMyOptionFun(sqrtMyOption))(MySome(-8))
```

실행 결과는 다음과 같다.
```
scala> val x5 = o(mkMyOptionFun(doubleMyOption), mkMyOptionFun(sqrtMyOption))(MySome(8))
x5: MyOption[Int] = MySome(4)

scala> val errval = o(mkMyOptionFun(doubleMyOption), mkMyOptionFun(sqrtMyOption))(MySome(-8))
errval: MyOption[Int] = MyNone
```

다음은 `Lazy`이다. 다만 `mkLazyFun`등이 인자로 `=>Int`를 받으므로(call-by-name), 
이를 타입을 맞추기 위해 에타 확장(eta-expansion)해서 `(x)=>doubleLazy(x)`로 만든다.

```
def mkLazyFun(f: (Int=>Lazy[Int])) = (x: Lazy[Int]) => {
	def value = x.getValue // x서 내부의 값 노출시키기(def로 했으므로 계산하지 않음)
	def tmpFun() = {	     // x 내부 값을 계산하지 않게 함수를 하나 정의
		val y = f(value)
		y.getValue()
	}
	new Lazy(tmpFun)
}

val x6 = o(mkLazyFun((x)=>doubleLazy(x)), mkLazyFun((y)=>sqrtLazy(y)))(initLazy(8))
x6.getValue()
```

실행결과는 다음과 같다.

```
scala> val x6 = o(mkLazyFun((x)=>doubleLazy(x)), mkLazyFun((y)=>sqrtLazy(y)))(initLazy(8))
x6: Lazy[Int] = Lazy@2346d950

scala> x6.getValue()
lazy double(8) run
lazy sqrt(16) run
res15: Int = 4
```

`getValue()`를 하기 전까지 아무것도 실행되지 않았음에 유의하라.

다음은 리스트를 할 차례이다. 리스트의 경우 원소가 여럿 있을 수 있으므로 도우미 함수를 정의해 사용한다. 
함수 내부에서 함수 정의가 가능하므로 외부 네임스페이스가 오염되는 일은 없다.

```
// 리스트
def mkMyListFun(f:Int=>MyList[Int]) = (x:MyList[Int]) => {
  // f가 만드는 리스트를 모두 합쳐야 하기 때문에 결과적으로는 
  // flatMap과 비슷한 일을 해야 한다.
  def append(l1:MyList[Int], l2:MyList[Int]):MyList[Int] = l1 match {
    case Cons(h,t) => {
      Cons(h,append(t,l2))
    }
    case MyNil => l2
  }
  def mapAll(l:MyList[Int]):MyList[Int] = l match {
    case Cons(h,t) => {
      val value2 = f(h)
      val remain = mapAll(t)
      append(value2,remain)
    }
    case MyNil => MyNil
  }
  mapAll(x)
}

// x1은 앞에서 만들었던 리스트이다.
// x1 = Cons(1,Cons(2,Cons(8,MyNil)))
val x7 = o(mkMyListFun(doubleMyList), mkMyListFun(sqrtMyList))(x1)
```

실행 결과는 다음과 같다.

```
scala> val x7 = o(mkMyListFun(doubleMyList), mkMyListFun(sqrtMyList))(x1)
x7: MyList[Int] = Cons(1,Cons(2,Cons(4,MyNil)))
```

리스트의 각 원소에 대해 `o(double, sqrt)`이 잘 계산되어 있다는 사실을 알 수 있다.

## 조합에 필요한 조건 생각해보기

그럼 과연 위 `mkCFun`을 주의깊게 설계할 때 지켜야 할 지침은 무엇일까?

먼저, `f: T=>C[U]`와 `mkCFun: (T=>C[U]) => C[T]) => C[V]`를 살펴보자.

`f`는 `T` 타입의 값에 어떤 변환을 하고, 기타 추가 정보를 `C`에 기록한 것이다.
`mkCFun(f)`를 하면 `C[T]=>C[U]` 타입의 함수가 생긴다. 이떄 `C[T]`에 있는 정보는 
`C[U]`에 병합되도록 `mkCFun`이 주의깊게 만들어져야 한다.

자, 이제 앞의 기본생성함수를 생각해 보자. 이 기본생성함수는 _타입은 맞춰주지만 부가정보는 최소만 기록_ 하는 
함수이다. 그런데 재미있는 사실은 기본생성함수 자체도 타입이 `T=>C[T]`라는 것이다. 그렇다면 이 함수를 
적용할 수 있는 곳이 두군데가 생긴다. 아래 설명에서 `x`는 기저타입(`T` 등), `c`는 `C[T]`등의 `C` 클래스 값이라는
사실을 일러둔다.

1. `mkCFun(initC)`를 생각해 보자. 이 함수는 `C[T]=>C[T]` 타입이 된다. 
이 경우 추가되는 정보가 없어야 할 것이다. 따라서 가능하면 원래의 값과 동일한 값이 
반환되면 좋다. _즉, `mkCFun(initC)(c) == c`여야 한다._

2. `mkCFun(f)(initC(x))`를 생각해 보자. 이 경우 `initC(x)`는 타입은 `C[T]` 이지만, 
정보는 최소한만 있는 것이다. 다시 여기에 `mkCFun(f)`가 적용었다. 
따라서 가능하면 이때 생기는 결과 `C[U]`에 있는 정보는 `f(x)`에서 만들어지는 정보와 동일하면 좋다. 
_즉, `mkCFun(f)(initC(x)) == f(x)` 여야 한다._

3. 두 함수의 합성을 생각해보자. 함수 `f: T=>C[U]`, `g: U=>C[V]`가 있다면, `mkCFun(g)(mkCFun(f)(c))`는 `f`를 `c`에 적용한
결과 생기는 정보에 다시 `g`를 적용한 것이다. 정보에 있어 순서가 중요할 수도 있다는 점을 생각해 보자. 
`(x)=>mkCFun(g)(f(x))`은 기저타입 `x`에 `f`와 `g`를 순서대로 적용하면서 얻은 정보를 누적시킨 결과를 반환하는 함수가 된다. 
그런데, 이 함수는 타입이 `T=>C[V]`라서, 정확히 `mkCFun`의 첫 인자가 될 수 있다. `mkCFun((x)=>mkCFun(g)(f(x)))`은 
`C[T]=>C[T]` 타입의 함수가 된다. 여기에 `C[T]`타입의 값 `c`를 적용시킨 `mkCFun((x)=>mkCFun(g)(f(x)))(c)`는, `c`에 있던 정보가 누락되어서는 안되고, 
`c`에 있던 정보, `f`, `g`가 순서대로 적용되며 누적된 정보가 다 순서대로(`c`, `f` 추가분, `g` 추가분) 누적되어 최종 
결과가 나와야 한다. 그런데 이 순서대로 누적해 정보를 변환시키는 것을 표현한다면 바로 `mkCFun(g)(mkCFun(f)(c))`이다. 
_따라서, `mkCFun(g)(mkCFun(f)(c)) = mkCFun((x)=>mkCFun(g)(f(x)))(c)`여야 한다._


## 조합하기 더 편하게 정리하기

이제 앞에서 조합에 대해 살펴봤던 것을 더 일반화하고 자세히 분석해 보자. 
정적 타이핑 프로그램 분석에서 가장 중요한 것 중 하나는 타입을 보는 것이다.
타입 중심으로 살펴 나가자.

`mkCFun`의 타입을 살펴보면 다음과 같다.

```
mkCFun: (T=>C[U]) => C[T] => C[V]
```

`f`, `g`가 각각 `T=>C[U]`, `U=>C[V]` 타입이라고 할 떄 `mkCFun`을 사용해 이 두 함수를 
합성(타입을 보며 알겠지만 `f`의 결과에 'g'를 적용하는 것이라 할 수 있다)하려면 다음과 같이 해야 한다.

```
mkCFun(g)(f(x:T)) : T=>C[V]
```

여기에서 `mkCFun`을 마치 2항연산자처럼 사용할수 있다면 어떨까?

```
g mkCFun (f(x:T)) 
```

만약 `h: V=>C[W]`가 있어서 `h(g(f)))`와 비슷한 합성을 원한다면?
```
mkCFun(h)(mkCFun(g)(f(x:T))) : T=>C[V]
```

이를 다시 2항연산형태로 표현하면 다음과 같다.

```
h mkCFun (g mkCFun (f(x:T)))
```

괄호를 보면 결합법칙이 오른쪽으로 성립함을 볼 수 있다. 그런데, 순서를 바꾸면 어떻게 될까?

```
mkCFun2: C[T] => (T=>C[U]) => C[V]
```

함수호출식으로 하면 다음과 같이 된다.
```
mkCFun2(f(x))(g)
mkCFun2(mkCFun2(f(x))(g))(h)
```

이항연산식으로 표현하면 다음과 같이 된다.

```
f(x) mkCFun2 g 

f(x) mkCFun2 g mkCFun2 h
```

몇 가지 측면에서 이렇게 하는 것이 도움이 된다.

1. 스칼라에서는 이항연산자를 만들려면 클래스의 메소드로 정의해야 한다. `C[T]`의 메소드로 `mkCFun2`를 
구현하면 깔끔하게 위와 같이 이항연산식을 사용할 수 있다.

2. `mkCFun2`가 클래스의 메소드가 되면 클래스 내부 정보를 마음대로 활용 가능해진다. 
해당 클래스의 객체를 만들때 필요한 추가정보를 클래스 내부에서 볼 수 있으므로 
여러가지로 편리하다. 물론 정보은닉 등 OOP의 기본적인 부분은 말할 것도 없다.

3. 상위 클래스/트레잇에서 잘 정의하면 하위 클래스는 상속만으로도 이런 기능을 잘 활용 가능하다.


## 모나드와 엮기

앞절에서 이항연산자 형태로 정리하는 것의 잇점에 대해 몇가지 이야기를 했다.

덤으로, 앞에서 설명했던 `mkCFun`의 성격을 이항연산자 형태로 정리하면 다음과 같이 괄호가 줄어들어 깔끔해진다.

_감싸기규칙3. `c mkCFun initC == c`여야 한다._

_감싸기규칙4. `initC(x) mkCFun f == f(x)`여야 한다._

_감싸기규칙5. `c mkCFun f mkCFun g =  c mkCFun {(x)=> f(x) mkCFun g}`이어야 한다._

거기에 세가지 감싸기규칙을 추가해 보자.

_감싸기 규칙0: 어떤 타입 `T`에 추가 정보를 더한 것이 C[T] 클래스이다. 당연하 추가 정보에 따라 클래스 구현은 달라진다._

_감싸기 규칙1: 어떤 타입 `T`를 최소한의 추가 정보와 함께 감싼 `C[T]`타입의 객체를 만드는 `T=>C[T]` 타입의 기본 생성 함수 가 있어야 한다._

_감싸기 규칙2: 정보가 추가된 `T=>C[T]`타입의 끼리 합성하기 위해서는 `T=>C[T]` 타입을 `C[T]=>C[T]`로 바꿔주는 고차함수가 필요한데, 이 함수는 `C`의 종류에 맞게 `주의깊게` 설계해야 한다._

_규칙2_ 를 세분화한것이 _규칙3_ ~ _규칙5_ 이다. 실제로 라이브러리 개발시에는 _규칙2_를 염두에 두고 개발하고, 검증시 3번~5번 규칙을 활용하면 될 것이다.

_규칙1_은 사실은 `initC` 함수를 어떻게 만들지에 대한 조언이다. _규칙0_을 염두에 두고 어떤 정보를 어떻게 보관할지 
고민한다면 _규칙1_도 크게 무리없이 구현할 수 있을 것이다. 

`mkCFun`을 `bind`나 `>>=`, `initC`를 `unit`이나 `return`이라고 부르면 규칙 3~5는 바로 모나드 규칙이 된다.

1. `(return x) >>= f == f x`

2. `m >>= return == m`

3. `(m >>= f) >>= g === >>= ( \x -> (f x >>= g))`

다음 글에서는 이를 기반으로 스칼라에서 하스켈 스타일 모나드를 구현해보고, 다시 스칼라의 모나드를 formulation해볼 것이다.





